package com.legalease.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbSchemaSetup implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbSchemaSetup.class);

    private final JdbcTemplate jdbcTemplate;

    public DbSchemaSetup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database schema setup and pgvector validation...");
        try {
            // 1. Enable pgvector extension
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
            log.info("pgvector extension verified/created successfully.");

            // 2. Create doc_embeddings table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS doc_embeddings (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    doc_id UUID REFERENCES documents(id) ON DELETE CASCADE,
                    chunk_text TEXT NOT NULL,
                    chunk_index INT NOT NULL,
                    embedding vector(768),
                    created_at TIMESTAMPTZ DEFAULT now()
                );
            """);
            log.info("doc_embeddings table verified/created successfully.");

            // 2b. Ensure embedding column exists
            try {
                jdbcTemplate.execute("ALTER TABLE doc_embeddings ADD COLUMN IF NOT EXISTS embedding vector(768);");
                log.info("doc_embeddings embedding column verified/created.");
            } catch (Exception alterEmbedEx) {
                log.warn("Could not alter doc_embeddings table to add embedding column.", alterEmbedEx);
            }

            // 3. Create IVFFlat cosine similarity index
            try {
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS doc_embeddings_embedding_idx 
                    ON doc_embeddings USING ivfflat (embedding vector_cosine_ops);
                """);
                log.info("IVFFlat vector index verified/created successfully.");
            } catch (Exception indexEx) {
                log.warn("Could not create IVFFlat index. This can happen if table is empty or memory limits are tight. Skipping index creation...", indexEx);
            }

            // 4. Create conversations table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conversations (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
                    doc_id UUID REFERENCES documents(id) ON DELETE CASCADE,
                    messages TEXT DEFAULT '[]',
                    created_at TIMESTAMPTZ DEFAULT now(),
                    CONSTRAINT unique_user_doc UNIQUE (user_id, doc_id)
                );
            """);
            log.info("conversations table verified/created successfully.");

            // 5. Alter doc_analyses to add compliance_report column
            jdbcTemplate.execute("ALTER TABLE doc_analyses ADD COLUMN IF NOT EXISTS compliance_report TEXT;");
            log.info("doc_analyses compliance_report column verified/created.");

            // 6. Create templates table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS templates (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    slug VARCHAR(255) UNIQUE NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    category VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    variables TEXT NOT NULL,
                    is_public BOOLEAN DEFAULT true,
                    created_at TIMESTAMPTZ DEFAULT now()
                );
            """);
            log.info("templates table verified/created successfully.");

            // 6b. Ensure is_public default value and correct existing nulls
            try {
                jdbcTemplate.execute("ALTER TABLE templates ALTER COLUMN is_public SET DEFAULT true;");
                jdbcTemplate.execute("UPDATE templates SET is_public = true WHERE is_public IS NULL;");
                log.info("templates is_public column verified and updated successfully.");
            } catch (Exception ex) {
                log.warn("Could not alter is_public column or update templates: {}", ex.getMessage());
            }

            // 6c. Fix literal '\n' characters in templates content to actual newlines
            try {
                jdbcTemplate.execute("UPDATE templates SET content = REPLACE(content, '\\n', CHR(10));");
                log.info("templates literal newlines replaced with actual newlines successfully.");
            } catch (Exception newlineEx) {
                log.warn("Could not replace literal newlines in templates: {}", newlineEx.getMessage());
            }

            // 7. Seed standard templates
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM templates", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("""
                    INSERT INTO templates (id, slug, title, category, content, variables, is_public, created_at) VALUES (
                        gen_random_uuid(),
                        'rental-agreement-ne',
                        'घर बहाल सम्झौता (Rental Agreement)',
                        'RENTAL',
                        'घर बहाल सम्झौता\\n\\nप्रथम पक्ष (घरधनी): {{landlord_name}}\\nदोस्रो पक्ष (बहालमा लिने): {{tenant_name}}\\n\\nतपसीलका शर्तहरूको अधीनमा रही दुवै पक्ष यस सम्झौतामा सहमत भएका छन्:\\n१. बहाल लिने घर/कोठाको ठेगाना: {{property_address}}\\n२. मासिक बहाल रकम: रू. {{rent_amount}}\\n३. धरौटी रकम (Security Deposit): रू. {{security_deposit}}\\n४. सम्झौता अवधि: मिति {{start_date}} देखि मिति {{end_date}} सम्म।\\n\\nघरधनी हस्ताक्षर: ______________\\nबहालवाला हस्ताक्षर: ______________',
                        '[{"name":"landlord_name","label":"घरधनीको नाम (Landlord Name)","type":"text","placeholder":"उदा: राम बहादुर श्रेष्ठ"},{"name":"tenant_name","label":"बहालमा लिनेको नाम (Tenant Name)","type":"text","placeholder":"उदा: हरि प्रसाद शर्मा"},{"name":"property_address","label":"घर/कोठाको ठेगाना (Property Address)","type":"text","placeholder":"उदा: काठमाडौं-३०, अनामनगर"},{"name":"rent_amount","label":"मासिक बहाल रकम (Monthly Rent Amount)","type":"number","placeholder":"उदा: १५०००"},{"name":"security_deposit","label":"धरौटी रकम (Security Deposit)","type":"number","placeholder":"उदा: ३००००"},{"name":"start_date","label":"सम्झौता सुरु हुने मिति (Start Date)","type":"date","placeholder":""},{"name":"end_date","label":"सम्झौता समाप्त हुने मिति (End Date)","type":"date","placeholder":""}]',
                        true,
                        now()
                    );
                """);

                jdbcTemplate.execute("""
                    INSERT INTO templates (id, slug, title, category, content, variables, is_public, created_at) VALUES (
                        gen_random_uuid(),
                        'employment-contract-ne',
                        'रोजगारी सम्झौता (Employment Contract)',
                        'EMPLOYMENT',
                        'रोजगारी सम्झौता पत्र\\n\\nनियोक्ता (Employer): {{employer_name}}\\nकर्मचारी (Employee): {{employee_name}}\\n\\nयस सम्झौताका शर्तहरू निम्न बमोजिम छन्:\\n१. पद (Job Title): {{job_title}}\\n२. तलब/पारिश्रमिक: मासिक रू. {{salary_amount}}\\n३. सेवा सुरु हुने मिति: {{start_date}}\\n४. परीक्षण काल (Probation Period): {{probation_months}} महिना।\\n५. काम गर्ने समय: दैनिक ८ घण्टा र हप्ताको ४८ घण्टा हुनेछ।\\n६. विदा तथा अन्य सुविधाहरू नेपालको श्रम ऐन २०७४ बमोजिम हुनेछन्।\\n\\nनियोक्ता हस्ताक्षर: ______________\\nकर्मचारी हस्ताक्षर: ______________',
                        '[{"name":"employer_name","label":"कम्पनी/नियोक्ताको नाम (Employer/Company Name)","type":"text","placeholder":"उदा: टेक सोलुसन्स प्रा. लि."},{"name":"employee_name","label":"कर्मचारीको नाम (Employee Name)","type":"text","placeholder":"उदा: सुनिता कुमारी महर्जन"},{"name":"job_title","label":"पद (Job Title)","type":"text","placeholder":"उदा: सफ्टवेयर इन्जिनियर"},{"name":"salary_amount","label":"मासिक तलब (Monthly Salary Amount)","type":"number","placeholder":"उदा: ५००००"},{"name":"start_date","label":"काम सुरु हुने मिति (Start Date)","type":"date","placeholder":""},{"name":"probation_months","label":"परीक्षण काल (Probation Period in Months)","type":"number","placeholder":"उदा: ६"}]',
                        true,
                        now()
                    );
                """);

                jdbcTemplate.execute("""
                    INSERT INTO templates (id, slug, title, category, content, variables, is_public, created_at) VALUES (
                        gen_random_uuid(),
                        'mutual-nda-ne',
                        'आपसी गोपनीयता सम्झौता (Mutual NDA)',
                        'NDA',
                        'आपसी गोपनीयता सम्झौता (Mutual NDA)\\n\\nयो सम्झौता मिति {{effective_date}} मा निम्न पक्षहरू बीच सम्पन्न भएको छ:\\nपक्ष क: {{party_a_name}}\\nपक्ष ख: {{party_b_name}}\\n\\nगोप्य सूचनाहरू आदानप्रदान गर्न र उक्त सूचनाहरूको गोपनीयता कायम राख्न सहमत छन्।\\n१. यो सम्झौताको अवधि {{duration_years}} वर्षको हुनेछ।\\n२. कुनै पनि पक्षले अर्को पक्षको लिखित सहमति बिना गोप्य सूचना तेस्रो पक्षलाई खुलासा गर्ने छैन।\\n\\nपक्ष क हस्ताक्षर: ______________\\nपक्ष ख हस्ताक्षर: ______________',
                        '[{"name":"party_a_name","label":"पहिलो पक्षको नाम (Party A Name)","type":"text","placeholder":"उदा: नेपाल सफ्टवेयर प्रा. लि."},{"name":"party_b_name","label":"दोस्रो पक्षको नाम (Party B Name)","type":"text","placeholder":"उदा: रमेश थापा"},{"name":"effective_date","label":"सम्झौता लागू हुने मिति (Effective Date)","type":"date","placeholder":""},{"name":"duration_years","label":"गोपनीयता अवधि वर्षमा (Duration in Years)","type":"number","placeholder":"उदा: ३"}]',
                        true,
                        now()
                    );
                """);

                jdbcTemplate.execute("""
                    INSERT INTO templates (id, slug, title, category, content, variables, is_public, created_at) VALUES (
                        gen_random_uuid(),
                        'promissory-note-ne',
                        'कपाली तमसुक / ऋण सम्झौता (Promissory Note)',
                        'LOAN',
                        'ऋण सम्झौता पत्र (तमसुक)\\n\\nऋण दिने (साहु): {{creditor_name}}\\nऋण लिने (आसामी): {{debtor_name}}\\n\\nम आसामीले आजको मितिमा साहुबाट रू. {{loan_amount}} ऋण लिएको छु र निम्न शर्तहरूमा बुझाउन मञ्जुर गर्दछु:\\n१. ब्याजदर: वार्षिक {{interest_rate}}% का दरले ब्याज बुझाउनेछु।\\n२. भाका मिति: मिति {{due_date}} भित्र सम्पूर्ण सावाँ र ब्याज चुक्ता गर्नेछु।\\n\\nऋण दिनेको हस्ताक्षर: ______________\\nऋण लिनेको हस्ताक्षर: ______________',
                        '[{"name":"creditor_name","label":"ऋण दिनेको नाम (Creditor/Lender Name)","type":"text","placeholder":"उदा: श्याम लाल श्रेष्ठ"},{"name":"debtor_name","label":"ऋण लिनेको नाम (Debtor/Borrower Name)","type":"text","placeholder":"उदा: गोमा देवी अधिकारी"},{"name":"loan_amount","label":"ऋण रकम (Loan Amount)","type":"number","placeholder":"उदा: १०००००"},{"name":"interest_rate","label":"वार्षिक ब्याज दर % मा (Annual Interest Rate %)","type":"number","placeholder":"उदा: १०"},{"name":"due_date","label":"बुझाउनु पर्ने अन्तिम मिति (Due Date)","type":"date","placeholder":""}]',
                        true,
                        now()
                    );
                """);
                log.info("Default legal templates pre-populated in database.");
            }

        } catch (Exception e) {
            log.error("Fatal error during database schema setup", e);
        }
    }
}
