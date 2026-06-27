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
            try {
                jdbcTemplate.execute("DELETE FROM templates;");
                log.info("Cleared old templates to ensure real templates are seeded.");
            } catch (Exception deleteEx) {
                log.warn("Could not delete from templates: {}", deleteEx.getMessage());
            }

            jdbcTemplate.execute("""
                INSERT INTO templates (id, slug, title, category, content, variables, is_public, created_at) VALUES (
                    gen_random_uuid(),
                    'rental-agreement-ne',
                    'घर बहाल सम्झौता (Rental Agreement)',
                    'RENTAL',
                    'घर बहाल सम्झौता पत्र\\n\\nलिखतम् प्रथम पक्ष (घरधनी/घर बहाल दिने):\\nनाम, थर: {{landlord_name}}\\nठेगाना: {{landlord_address}}\\nनागरिकता नं.: {{landlord_citizenship_no}}\\nबाबुको नाम: {{landlord_father_name}}\\nबाजेको नाम: {{landlord_grandfather_name}}\\n(यस सम्झौतामा यसपछि ''प्रथम पक्ष'' भनिएको छ ।)\\n\\nतथा\\n\\nदोस्रो पक्ष (घर बहालमा लिने):\\nनाम, थर: {{tenant_name}}\\nठेगाना: {{tenant_address}}\\nनागरिकता नं.: {{tenant_citizenship_no}}\\nबाबुको नाम: {{tenant_father_name}}\\nबाजेको नाम: {{tenant_grandfather_name}}\\n(यस सम्झौतामा यसपछि ''दोस्रो पक्ष'' भनिएको छ ।)\\n\\nहामी प्रथम पक्ष र दोस्रो पक्ष बीच आपसी सहमतिमा प्रथम पक्षको स्वामित्वमा रहेको तपसील बमोजिमको घर/कोठा दोस्रो पक्षलाई बहालमा दिन/लिन मञ्जुर भई देहायका शर्तहरूको अधीनमा रहने गरी यो सम्झौता पत्र लिखित गरी हस्ताक्षर गरेका छौँ।\\n\\nसम्झौताका शर्तहरू:\\n१. बहाल लिने सम्बन्धी विवरण: प्रथम पक्षको स्वामित्वमा रहेको {{property_address}} मा अवस्थित घरको {{rented_rooms_count}} दोस्रो पक्षले आवासीय/व्यावसायिक प्रयोजनका लागि बहालमा लिएको छ।\\n२. सम्झौता अवधि र नविकरण: यो सम्झौता मिति {{start_date}} देखि लागू भई मिति {{end_date}} सम्म जम्मा {{duration_months}} महिनाका लागि कायम रहनेछ। सम्झौताको अवधि समाप्त भएपछि आपसी सहमतिमा नवीकरण गर्न सकिनेछ।\\n३. मासिक बहाल रकम र कर: दोस्रो पक्षले प्रथम पक्षलाई मासिक बहाल रकम रू. {{rent_amount}} (अक्षरेपी {{rent_amount_words}} मात्र) बुझाउनुपर्नेछ। प्रचलित कानून बमोजिम लाग्ने घर बहाल कर {{tax_payer}} ले भुक्तानी गर्नेछ।\\n४. बहाल भुक्तानी अवधि: दोस्रो पक्षले प्रत्येक महिनाको बहाल रकम अर्को महिनाको {{payment_due_day}} गतेभित्र प्रथम पक्षलाई बुझाइसक्नुपर्नेछ।\\n५. धरौटी रकम (Security Deposit): दोस्रो पक्षले प्रथम पक्षलाई सुरक्षा धरौटी बापत रू. {{security_deposit}} (अक्षरेपी {{security_deposit_words}} मात्र) सम्झौता हुँदाका बखत बुझाएको छ। यो धरौटी सम्झौता समाप्त भई कोठा खाली गर्दा विना ब्याज फिर्ता गरिनेछ।\\n६. पानी, बिजुली र अन्य महसुल: बहालमा लिएको अवधिभरको बिजुली, खानेपानी, फोहोरमैला व्यवस्थापन र इन्टरनेटको महसुल दोस्रो पक्ष आफैँले तिर्नुपर्नेछ।\\n७. बहाल वृद्धि: यदि सम्झौता अवधि थप गरिएमा वा लामो समयको भएमा प्रत्येक २ वर्षमा बहाल रकम १०% (दश प्रतिशत) का दरले वृद्धि गरिनेछ।\\n८. घर खाली गर्ने र सूचना अवधि: कुनै पनि पक्षले सम्झौता अवधि अगावै घर खाली गर्न वा गराउन चाहेमा ३५ (पैंतीस) दिन अगाडि नै लिखित रूपमा अर्को पक्षलाई जानकारी (पूर्व सूचना) दिनुपर्नेछ।\\n९. क्षति तथा मर्मत सम्भार: बहालमा बस्दा भौतिक संरचनामा कुनै नोक्सानी पुगेमा सो को मर्मत दोस्रो पक्ष आफैँले गर्नुपर्नेछ। सामान्य मर्मत सम्भार दोस्रो पक्षले र संरचनागत ठूला मर्मतहरू प्रथम पक्षले गर्नेछ।\\n\\nतपसील बमोजिमका शर्तहरू मञ्जुर गरी हामी दुवै पक्षले यो सम्झौता पत्रमा हस्ताक्षर गरी एक-एक प्रति बुझिलियौँ।\\n\\nप्रथम पक्ष (घरधनी) को हस्ताक्षर: _______________\\nदस्तखत मिति: {{agreement_date}}\\n\\nदोस्रो पक्ष (बहालवाला) को हस्ताक्षर: _______________\\nदस्तखत मिति: {{agreement_date}}\\n\\nसाक्षीहरू:\\n१. नाम: ______________________ हस्ताक्षर: _______________\\n२. नाम: ______________________ हस्ताक्षर: _______________',
                    '[{"name":"landlord_name","label":"घरधनीको नाम (Landlord Name)","type":"text","placeholder":"उदा: राम बहादुर श्रेष्ठ"},{"name":"landlord_address","label":"घरधनीको ठेगाना (Landlord Address)","type":"text","placeholder":"उदा: पोखरा-८, कास्की"},{"name":"landlord_citizenship_no","label":"घरधनीको नागरिकता नं. (Landlord Citizenship No)","type":"text","placeholder":"उदा: ४५६/३८२९"},{"name":"landlord_father_name","label":"घरधनीको बुबाको नाम (Landlord Father Name)","type":"text","placeholder":"उदा: हरि बहादुर श्रेष्ठ"},{"name":"landlord_grandfather_name","label":"घरधनीको बाजेको नाम (Landlord Grandfather Name)","type":"text","placeholder":"उदा: कृष्ण बहादुर श्रेष्ठ"},{"name":"tenant_name","label":"बहालमा लिनेको नाम (Tenant Name)","type":"text","placeholder":"उदा: हरि प्रसाद शर्मा"},{"name":"tenant_address","label":"बहालवालाको ठेगाना (Tenant Address)","type":"text","placeholder":"उदा: भरतपुर-१०, चितवन"},{"name":"tenant_citizenship_no","label":"बहालवालाको नागरिकता नं. (Tenant Citizenship No)","type":"text","placeholder":"उदा: ९८२-३८२९१"},{"name":"tenant_father_name","label":"बहालवालाको बुबाको नाम (Tenant Father Name)","type":"text","placeholder":"उदा: गणेश प्रसाद शर्मा"},{"name":"tenant_grandfather_name","label":"बहालवालाको बाजेको नाम (Tenant Grandfather Name)","type":"text","placeholder":"उदा: मदन प्रसाद शर्मा"},{"name":"property_address","label":"घर/कोठाको ठेगाना (Property Address)","type":"text","placeholder":"उदा: काठमाडौं-३०, अनामनगर"},{"name":"rented_rooms_count","label":"बहाल लिने कोठा/फ्ल्याट संख्या (No. of Rooms/Flat)","type":"text","placeholder":"उदा: ३ वटा कोठा"},{"name":"rent_amount","label":"मासिक बहाल रकम (Monthly Rent Amount)","type":"number","placeholder":"उदा: १५०००"},{"name":"rent_amount_words","label":"मासिक बहाल रकम अक्षरेपी (Rent in Words)","type":"text","placeholder":"उदा: पन्ध्र हजार"},{"name":"security_deposit","label":"धरौटी रकम (Security Deposit)","type":"number","placeholder":"उदा: ३००००"},{"name":"security_deposit_words","label":"धरौटी रकम अक्षरेपी (Deposit in Words)","type":"text","placeholder":"उदा: तीस हजार"},{"name":"tax_payer","label":"घर बहाल कर भुक्तानी गर्ने (Tax Payer)","type":"text","placeholder":"उदा: प्रथम पक्ष / दोस्रो पक्ष"},{"name":"payment_due_day","label":"बहाल तिर्ने गते (Payment Due Day)","type":"text","placeholder":"उदा: ७"},{"name":"start_date","label":"सम्झौता सुरु हुने मिति (Start Date)","type":"date","placeholder":""},{"name":"end_date","label":"सम्झौता समाप्त हुने मिति (End Date)","type":"date","placeholder":""},{"name":"duration_months","label":"बहाल अवधि महिनामा (Duration in Months)","type":"number","placeholder":"उदा: १२"},{"name":"agreement_date","label":"सम्झौता मिति (Agreement Date)","type":"date","placeholder":""}]',
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
                    'रोजगारी सम्झौता पत्र\\n(श्रम ऐन, २०७४ को दफा ११ बमोजिम)\\n\\nयो रोजगारी सम्झौता पत्र आज मिति {{agreement_date}} का दिन निम्न रोजगारदाता र श्रमिक बीच आपसी सहमतिमा सम्पन्न भएको छ।\\n\\nरोजगारदाता (प्रतिष्ठानको विवरण):\\nप्रतिष्ठान/कम्पनीको नाम: {{employer_name}}\\nठेगाना: {{employer_address}}\\nदर्ता नम्बर: {{employer_reg_no}}\\nप्रतिनिधिको नाम र पद: {{employer_representative}}\\n(यस सम्झौतामा यसपछि ''रोजगारदाता'' भनिएको छ ।)\\n\\nर\\n\\nश्रमिक/कर्मचारीको विवरण:\\nनाम, थर: {{employee_name}}\\nस्थायी ठेगाना: {{employee_address}}\\nनागरिकता वा पासपोर्ट नं.: {{employee_citizenship_no}}\\nबाबुको नाम: {{employee_father_name}}\\nबाजेको नाम: {{employee_grandfather_name}}\\n(यस सम्झौतामा यसपछि ''श्रमिक'' भनिएको छ ।)\\n\\nदेहायका शर्तहरूको अधीनमा रही रोजगारदाताले श्रमिकलाई रोजगारीमा संलग्न गराउन र श्रमिकले रोजगारदाताको अधीनमा रही कार्य गर्न मञ्जुर भई यो सम्झौता गरिएको छ:\\n\\nसम्झौताका शर्तहरू:\\n१. पद र कार्य विवरण: श्रमिकलाई {{job_title}} को पदमा नियुक्त गरिएको छ। श्रमिकको main कार्य विवरण र जिम्मेवारी रोजगारदाताले तोके बमोजिम तथा प्रतिष्ठानको हित अनुकूल हुनेछ।\\n२. रोजगारीको प्रकृति: यो रोजगारी सम्झौता श्रम ऐन, २०७४ को व्यवस्था बमोजिम {{employment_nature}} (नियमित/समयगत/कार्यगत/आकस्मिक) प्रकृतिको हुनेछ।\\n३. रोजगारी सुरु मिति र परीक्षण काल (Probation Period): रोजगारी मिति {{start_date}} देखि लागू हुनेछ। श्रम ऐन, २०७४ को दफा १३ बमोजिम श्रमिकको परीक्षण काल {{probation_months}} महिना (अधिकतम ६ महिना) को रहनेछ।\\n४. कार्य समय र ओभरटाइम: श्रमिकले दैनिक ८ घण्टा र हप्ताको ४८ घण्टा कार्य गर्नुपर्नेछ। सो भन्दा बढी कार्य गराउनु परेमा श्रम ऐन, २०७४ बमोजिम थप पारिश्रमिक (ओभरटाइम) प्रदान गरिनेछ।\\n५. पारिश्रमिक तथा सुविधाहरू:\\nक) मासिक आधारभूत तलब (Basic Salary): रू. {{basic_salary}}\\nख) मासिक महँगी भत्ता (Dearness Allowance): रू. {{dearness_allowance}}\\nग) अन्य सुविधाहरू (भत्ता/यातायात आदि): रू. {{other_allowance}}\\nघ) जम्मा मासिक पारिश्रमिक: रू. {{total_salary}} (अक्षरेपी {{total_salary_words}} मात्र)।\\n६. सामाजिक सुरक्षा र कोष योगदान: श्रम ऐन, २०७४ र योगदानमा आधारित सामाजिक सुरक्षा ऐन, २०७४ को प्रावधान बमोजिम रोजगारदाताले आधारभूत पारिश्रमिकको १०% सञ्चय कोष र ८.३३% उपदान (Gratuity) बापतको रकम सामाजिक सुरक्षा कोष (SSF) मा जम्मा गर्ने व्यवस्था मिलाउनेछ।\\n७. विदा र सुविधाहरू: श्रमिकले श्रम ऐन, २०७४ तथा नियमावली बमोजिम साप्ताहिक बिदा, सार्वजनिक बिदा, घर बिदा, बिरामी बिदा, प्रसूति/प्रसूति स्याहार बिदा, र किरिया बिदा पाउन योग्य हुनेछन्।\\n८. गोपनीयता र आचरण: श्रमिकले रोजगारदाताको व्यावसायिक गोप्य कुराहरू तेस्रो पक्षलाई खुलासा गर्ने छैनन् र कार्यालयको अनुशासन र मर्यादाको पालना गर्नेछन्।\\n९. रोजगारीको अन्त्य (Termination): रोजगारीको अन्त्य वा बर्खास्तगी श्रम ऐन, २०७४ को परिच्छेद ११ मा तोकिएको प्रक्रिया र व्यवस्था बमोजिम मात्र गरिनेछ। सम्झौता अन्त्य गर्नुपरेमा तोकिएको पूर्व-सूचना (Notice Period) दिनुपर्नेछ।\\n\\nहामी रोजगारदाता र श्रमिक दुवै पक्षले यस रोजगारी सम्झौता पत्रका सबै बुँदाहरू राम्ररी पढी, बुझी, मञ्जुर भई साक्षीहरूको रोहवरमा हस्ताक्षर गरी आ-आफ्नो एक-एक प्रति बुझिलियौँ।\\n\\nरोजगारदाताको तर्फबाट (हस्ताक्षर र छाप): _______________\\nनाम: {{employer_representative}}\\nपद: ______________________\\nमिति: {{agreement_date}}\\n\\nश्रमिकको तर्फबाट हस्ताक्षर: _______________\\nनाम: {{employee_name}}\\nमिति: {{agreement_date}}\\n\\nसाक्षीहरू:\\n१. नाम: ______________________ हस्ताक्षर: _______________\\n२. नाम: ______________________ हस्ताक्षर: _______________',
                    '[{"name":"agreement_date","label":"सम्झौता मिति (Agreement Date)","type":"date","placeholder":""},{"name":"employer_name","label":"प्रतिष्ठान/कम्पनीको नाम (Employer/Company Name)","type":"text","placeholder":"उदा: टेक सोलुसन्स प्रा. लि."},{"name":"employer_address","label":"कम्पनीको ठेगाना (Employer Address)","type":"text","placeholder":"उदा: बानेश्वर, काठमाडौं"},{"name":"employer_reg_no","label":"कम्पनी दर्ता नम्बर (Company Reg No)","type":"text","placeholder":"उदा: १२३४५/०७८"},{"name":"employer_representative","label":"कम्पनी प्रतिनिधि र पद (Representative Name & Title)","type":"text","placeholder":"उदा: राम प्रसाद रिजाल (प्रबन्ध निर्देशक)"},{"name":"employee_name","label":"श्रमिक/कर्मचारीको नाम (Employee Name)","type":"text","placeholder":"उदा: सुनिता कुमारी महर्जन"},{"name":"employee_address","label":"कर्मचारीको स्थायी ठेगाना (Employee Address)","type":"text","placeholder":"उदा: मध्यपुर ठिमी, भक्तपुर"},{"name":"employee_citizenship_no","label":"नागरिकता/पासपोर्ट नं (Citizenship/Passport No)","type":"text","placeholder":"उदा: ३४-०१-७५-०९८४"},{"name":"employee_father_name","label":"कर्मचारीको बुबाको नाम (Employee''''s Father Name)","type":"text","placeholder":"उदा: विकास कुमार महर्जन"},{"name":"employee_grandfather_name","label":"कर्मचारीको बाजेको नाम (Employee''''s Grandfather Name)","type":"text","placeholder":"उदा: लाल बहादुर महर्जन"},{"name":"job_title","label":"पद (Job Title)","type":"text","placeholder":"उदा: सफ्टवेयर इन्जिनियर"},{"name":"employment_nature","label":"रोजगारीको प्रकृति (Employment Nature)","type":"text","placeholder":"उदा: नियमित / समयगत"},{"name":"start_date","label":"काम सुरु हुने मिति (Start Date)","type":"date","placeholder":""},{"name":"probation_months","label":"परीक्षण काल महिनामा (Probation Period in Months)","type":"number","placeholder":"उदा: ६"},{"name":"basic_salary","label":"मासिक आधारभूत तलब (Monthly Basic Salary)","type":"number","placeholder":"उदा: ३५०००"},{"name":"dearness_allowance","label":"मासिक महँगी भत्ता (Monthly Dearness Allowance)","type":"number","placeholder":"उदा: १५०००"},{"name":"other_allowance","label":"मासिक अन्य भत्ता (Monthly Other Allowance)","type":"number","placeholder":"उदा: ५०००"},{"name":"total_salary","label":"जम्मा मासिक पारिश्रमिक (Total Monthly Salary)","type":"number","placeholder":"उदा: ५५०००"},{"name":"total_salary_words","label":"जम्मा मासिक पारिश्रमिक अक्षरेपी (Total Salary in Words)","type":"text","placeholder":"उदा: पचपन हजार"}]',
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
                    'आपसी गोपनीयता सम्झौता पत्र\\n(Mutual NDA)\\n\\nयो आपसी गोपनीयता सम्झौता पत्र आज मिति {{effective_date}} का दिन निम्न दुई पक्षहरू बीच आपसी व्यावसायिक सम्बन्ध स्थापना र गोप्य सूचनाको सुरक्षा गर्ने उद्देश्यले सम्पन्न भएको छ।\\n\\nपहिलो पक्ष:\\nकम्पनी/प्रतिष्ठानको नाम: {{party_a_name}}\\nठेगाना: {{party_a_address}}\\nप्रतिनिधिको नाम र पद: {{party_a_representative}}\\n(यस सम्झौतामा यसपछि ''पहिलो पक्ष'' भनिएको छ ।)\\n\\nतथा\\n\\nदोस्रो पक्ष:\\nकम्पनी/व्यक्ति को नाम: {{party_b_name}}\\nठेगाना: {{party_b_address}}\\nप्रतिनिधि/व्यक्तिको नाम: {{party_b_representative}}\\n(यस सम्झौतामा यसपछि ''दोस्रो पक्ष'' भनिएको छ ।)\\n\\nदुवै पक्षहरू व्यावसायिक सहकार्य, प्रविधि साझेदारी वा लगानीका अवसरहरूका बारेमा छलफल गर्न इच्छुक रहेका र सो क्रममा एक-अर्काका गोप्य र संवेदनशील व्यापारिक सूचनाहरू आदानप्रदान हुने भएकोले ती सूचनाहरूको गोपनीयता कायम राख्न देहाय बमोजिमका शर्तहरूमा मञ्जुर भएका छन्:\\n\\nसम्झौताका शर्तहरू:\\n१. गोप्य सूचनाको परिभाषा (Definition of Confidential Information): यस सम्झौताको प्रयोजनका लागि ''गोप्य सूचना'' भन्नाले लिखित, मौखिक, डिजिटल वा अन्य कुनै भी माध्यमबाट प्राप्त भएका व्यावसायिक योजना, वित्तीय विवरण, सफ्टवेयर कोड, ग्राहक विवरण, प्राविधिक डेटा, र व्यापारिक रणनीतिहरू सम्झनुपर्दछ।\\n२. गोपनीयताको दायित्व (Obligation of Confidentiality): कुनै पनि पक्षले अर्को पक्षको पूर्व लिखित अनुमति बिना प्राप्त भएका कुनै पनि गोप्य सूचना तेस्रो पक्षलाई खुलासा, प्रकाशन वा हस्तान्तरण गर्ने छैन। प्राप्त सूचनाहरू केवल तोकिएको सहकार्य प्रयोजनका लागि मात्र प्रयोग गरिनेछ।\\n३. सूचना सुरक्षा (Information Security): दुवै पक्षले प्राप्त गरेका गोप्य सूचनाहरूलाई आफ्नो स्वामित्वका सूचनाहरू सरह उच्च सतर्कता र सुरक्षाका साथ सुरक्षित राख्नेछन्।\\n४. अपवादहरू (Exceptions): यो गोपनीयताको दायित्व देहायका अवस्थाहरूमा लागू हुने छैन:\\nक) सूचना सार्वजनिक रूपमा पहिले नै उपलब्ध भएको अवस्थामा।\\nख) अर्को पक्षबाट प्राप्त हुनुभन्दा अगाडि नै आफ्नो जानकारीमा रहेको प्रमाण भएको अवस्थामा।\\nग) सक्षम अदालत वा सरकारी निकायको कानूनी आदेश बमोजिम खुलासा गर्नुपरेको अवस्थामा।\\n५. सम्झौताको अवधि (Duration): यो सम्झौता लागू भएको मिति {{effective_date}} देखि जम्मा {{duration_years}} वर्षसम्म पूर्ण रूपमा लागू रहनेछ र सम्झौता अवधि समाप्त भए पश्चात् पनि गोपनीयता सम्बन्धी दायित्व थप ३ वर्ष कायम रहनेछ।\\n६. उल्लंघनको परिणाम: यदि कुनै पक्षबाट सम्झौताको उल्लंघन भई हानि-नोक्सानी भएमा पीडित पक्षले क्षतिपूर्ति दाबी गर्न सक्नेछ र सो को विवाद निरूपण प्रचलित नेपाल कानून बमोजिम हुनेछ।\\n\\nहामी दुवै पक्ष यस सम्झौताका शर्तहरूमा पूर्ण सहमत भई साक्षीहरूको रोहवरमा यो सम्झौता पत्रमा हस्ताक्षर गरेका छौँ।\\n\\nपहिलो पक्षको तर्फबाट (हस्ताक्षर र छाप): _______________\\nनाम: {{party_a_representative}}\\nपद: ______________________\\nमिति: {{effective_date}}\\n\\nदोस्रो पक्षको तर्फबाट (हस्ताक्षर र छाप): _______________\\nनाम: {{party_b_representative}}\\nपद: ______________________\\nमिति: {{effective_date}}\\n\\nसाक्षीहरू:\\n१. नाम: ______________________ हस्ताक्षर: _______________\\n२. नाम: ______________________ हस्ताक्षर: _______________',
                    '[{"name":"effective_date","label":"लागू हुने मिति (Effective Date)","type":"date","placeholder":""},{"name":"party_a_name","label":"पहिलो पक्ष कम्पनीको नाम (Party A Company Name)","type":"text","placeholder":"उदा: नेपाल सफ्टवेयर प्रा. लि."},{"name":"party_a_address","label":"पहिलो पक्षको ठेगाना (Party A Address)","type":"text","placeholder":"उदा: काठमाडौं-१०, बानेश्वर"},{"name":"party_a_representative","label":"पहिलो पक्षका प्रतिनिधि र पद (Party A Representative)","type":"text","placeholder":"उदा: Rajesh Sharma (प्रबन्ध निर्देशक)"},{"name":"party_b_name","label":"दोस्रो पक्ष कम्पनी/व्यक्तिको नाम (Party B Name)","type":"text","placeholder":"उदा: रमेश थापा / टेक सोलुसन्स"},{"name":"party_b_address","label":"दोस्रो पक्षको ठेगाना (Party B Address)","type":"text","placeholder":"उदा: ललितपुर-३, कुपण्डोल"},{"name":"party_b_representative","label":"दोस्रो पक्षका प्रतिनिधि र पद (Party B Representative)","type":"text","placeholder":"उदा: रमेश थापा (सञ्चालक)"},{"name":"duration_years","label":"गोपनीयता अवधि वर्षमा (Duration in Years)","type":"number","placeholder":"उदा: ३"}]',
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
                    'कपाली तमसुक (Promissory Note)\\n\\nलिखतम् ऋणी (ऋण लिने आसामी):\\nनाम, थर: {{debtor_name}}\\nस्थायी ठेगाना: {{debtor_address}}\\nहालको ठेगाना: {{debtor_current_address}}\\nनागरिकता नं.: {{debtor_citizenship_no}}\\nबाबुको नाम: {{debtor_father_name}}\\nबाजेको नाम: {{debtor_grandfather_name}}\\n(यस लिखतमा यसपछि ''ऋणी'' भनिएको छ ।)\\n\\nतथा\\n\\nसाहु (ऋण दिने):\\nनाम, थर: {{creditor_name}}\\nस्थायी ठेगाना: {{creditor_address}}\\nनागरिकता नं.: {{creditor_citizenship_no}}\\nबाबुको नाम: {{creditor_father_name}}\\nबाजेको नाम: {{creditor_grandfather_name}}\\n(यस लिखतमा यसपछि ''साहु'' भनिएको छ ।)\\n\\nलिखितम् आगे म ऋणीले मेरो घरायसी व्यवहार चलाउन, आवश्यक व्यावहारिक कार्य फत्ते गर्न तथा व्यापार व्यवसाय विस्तार गर्ने प्रयोजनका निमित्त आजको मितिमा साहु {{creditor_name}} बाट नगद रू. {{loan_amount}} (अक्षरेपी {{loan_amount_words}} मात्र) ऋण लिएको छु।\\n\\nउक्त ऋण रकम प्राप्त गरी मैले बुझिलिएँ र यस लेनदेन सम्बन्धी देहायका शर्तहरूमा पूर्ण रूपमा मञ्जुर छु:\\n१. ब्याज दर: लिइएको ऋण रकमको ब्याज दर वार्षिक {{interest_rate}}% (प्रचलित कानूनको सीमा भित्र रहने गरी) का दरले बुझाउने छु।\\n२. भाका र भुक्तानी: यो ऋण रकम सावाँ र सो को ब्याज समेत जोडी आगामी मिति {{due_date}} भित्र म ऋणीले साहुलाई एकमुष्ट रूपमा बुझाउने छु।\\n३. भुक्तानी नगरेमा व्यवस्था: यदि मैले तोकिएको भाखाभित्र सावाँ तथा ब्याज बुझाउन नसकी उल्लंघन गरेमा, मेरो चल-अचल श्रीसम्पत्तिबाट प्रचलित कानून र मुलुकी देवानी संहिता बमोजिम असुल उपर गरी लिनुहोला भनी राजीखुशीले साक्षीहरूको रोहवरमा यो कपाली तमसुकको कागज लिखत तयार गरी आफ्नो सहीछाप र दायाँ-बायाँ बुढीऔँलाको छाप समेत लगाइ दिएको छु।\\n४. कानुनी प्रमाणीकरण: यो लिखत को कारोबारलाई प्रचलित कानून बमोजिम सम्बन्धित स्थानीय तह वा वडा कार्यालयमा दर्ता र प्रमाणित गराइनेछ।\\n\\nतपसील बमोजिमका साक्षीहरूको रोहवरमा यो तमसुकको लिखत तयार गरिएको हो।\\n\\nऋणी (ऋण लिने) को दस्तखत: _______________\\nनाम: {{debtor_name}}\\nदायाँ औँठा छाप: [       ]  बायाँ औँठा छाप: [       ]\\nमिति: {{agreement_date}}\\n\\nसाहु (ऋण दिने) को दस्तखत: _______________\\nनाम: {{creditor_name}}\\nमिति: {{agreement_date}}\\n\\nरोहवरमा बस्ने साक्षीहरू:\\n१. नाम: ______________________ ठेगाना: ________________ नागरिकता नं: _____________ हस्ताक्षर: _______________\\n२. नाम: ______________________ ठेगाना: ________________ नागरिकता नं: _____________ हस्ताक्षर: _______________',
                    '[{"name":"agreement_date","label":"तमसुक लेखिएको मिति (Agreement Date)","type":"date","placeholder":""},{"name":"debtor_name","label":"ऋणीको नाम (Debtor/Borrower Name)","type":"text","placeholder":"उदा: गोमा देवी अधिकारी"},{"name":"debtor_address","label":"ऋणीको स्थायी ठेगाना (Debtor Address)","type":"text","placeholder":"उदा: बनेपा-४, काभ्रेपलाञ्चोक"},{"name":"debtor_current_address","label":"ऋणीको हालको ठेगाना (Debtor Current Address)","type":"text","placeholder":"उदा: कोटेश्वर, काठमाडौं"},{"name":"debtor_citizenship_no","label":"ऋणीको नागरिकता नं. (Debtor Citizenship No)","type":"text","placeholder":"उदा: ८९२३७-८७३"},{"name":"debtor_father_name","label":"ऋणीको बुबाको नाम (Debtor''''s Father Name)","type":"text","placeholder":"उदा: केदार नाथ अधिकारी"},{"name":"debtor_grandfather_name","label":"ऋणीको बाजेको नाम (Debtor''''s Grandfather Name)","type":"text","placeholder":"उदा: लोक नाथ अधिकारी"},{"name":"creditor_name","label":"साहुको नाम (Creditor/Lender Name)","type":"text","placeholder":"उदा: श्याम लाल श्रेष्ठ"},{"name":"creditor_address","label":"साहुको स्थायी ठेगाना (Creditor Address)","type":"text","placeholder":"उदा: धुलिखेल-२, काभ्रे"},{"name":"creditor_citizenship_no","label":"साहुको नागरिकता नं. (Creditor Citizenship No)","type":"text","placeholder":"उदा: ९८३२-८७४३"},{"name":"creditor_father_name","label":"साहुको बुबाको नाम (Creditor''''s Father Name)","type":"text","placeholder":"उदा: राम लाल श्रेष्ठ"},{"name":"creditor_grandfather_name","label":"साहुको बाजेको नाम (Creditor''''s Grandfather Name)","type":"text","placeholder":"उदा: हरी लाल श्रेष्ठ"},{"name":"loan_amount","label":"ऋण सावाँ रकम (Loan Amount)","type":"number","placeholder":"उदा: १०००००"},{"name":"loan_amount_words","label":"ऋण सावाँ रकम अक्षरेपी (Loan Amount in Words)","type":"text","placeholder":"उदा: एक लाख"},{"name":"interest_rate","label":"वार्षिक ब्याज दर % मा (Annual Interest Rate %)","type":"number","placeholder":"उदा: १०"},{"name":"due_date","label":"बुझाउनु पर्ने भाखा मिति (Due Date)","type":"date","placeholder":""}]',
                    true,
                    now()
                );
            """);
            log.info("Default real legal templates pre-populated in database.");

            // 8. Seed verified lawyers if none exist
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lawyers", Integer.class);
                if (count == null || count == 0) {
                    log.info("Seeding default verified lawyers...");
                    jdbcTemplate.execute("""
                        INSERT INTO lawyers (id, name, email, phone, specialization, rating, hourly_rate, bio, location, experience_years, availability, is_verified, created_at)
                        VALUES (
                            gen_random_uuid(),
                            'Aarav Sharma',
                            'aarav@legalease.com',
                            '+977-9851012345',
                            'CIVIL',
                            4.8,
                            1500.00,
                            'Senior Advocate specializing in Nepalese Civil law, property disputes, and land registrations. Over 12 years of courtroom experience.',
                            'Kathmandu',
                            12,
                            '["2026-06-28 10:00", "2026-06-28 14:00", "2026-06-29 11:00", "2026-06-29 15:00"]',
                            true,
                            now()
                        );
                    """);
                    jdbcTemplate.execute("""
                        INSERT INTO lawyers (id, name, email, phone, specialization, rating, hourly_rate, bio, location, experience_years, availability, is_verified, created_at)
                        VALUES (
                            gen_random_uuid(),
                            'Pooja Adhikari',
                            'pooja@legalease.com',
                            '+977-9841234567',
                            'LABOUR',
                            4.9,
                            1200.00,
                            'Advocate focused on employment contracts, Labour Act 2074 compliance audit, corporate disputes, and employee rights.',
                            'Lalitpur',
                            8,
                            '["2026-06-28 09:00", "2026-06-28 13:00", "2026-06-30 10:00", "2026-06-30 16:00"]',
                            true,
                            now()
                        );
                    """);
                    jdbcTemplate.execute("""
                        INSERT INTO lawyers (id, name, email, phone, specialization, rating, hourly_rate, bio, location, experience_years, availability, is_verified, created_at)
                        VALUES (
                            gen_random_uuid(),
                            'Rajesh Shrestha',
                            'rajesh@legalease.com',
                            '+977-9810987654',
                            'CORPORATE',
                            4.7,
                            2000.00,
                            'Expert corporate lawyer specialized in startup advisory, foreign direct investment (FDI), copyright registrations, and commercial NDAs.',
                            'Kathmandu',
                            15,
                            '["2026-06-29 10:00", "2026-06-29 13:00", "2026-06-30 11:00", "2026-06-30 14:00"]',
                            true,
                            now()
                        );
                    """);
                    log.info("Default verified lawyers seeded successfully.");
                }
            } catch (Exception lawyerEx) {
                log.warn("Could not seed lawyers table: {}", lawyerEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Fatal error during database schema setup", e);
        }
    }
}
