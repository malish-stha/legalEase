"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@clerk/nextjs";
import axios from "axios";
import {
  FileText, Loader2, Sparkles, Scale, BookOpen,
  ArrowRight, ShieldCheck, HelpCircle
} from "lucide-react";
import Link from "next/link";
import { useSelector } from "react-redux";
import { RootState } from "../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

interface Template {
  id: string;
  slug: string;
  title: string;
  category: string;
  content: string;
  variables: string;
}

const categoryIcons = {
  RENTAL: "🏘️",
  EMPLOYMENT: "💼",
  NDA: "🔒",
  LOAN: "💰",
};

export default function TemplatesDirectory() {
  const { getToken } = useAuth();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchTemplates() {
      try {
        const token = await getToken();
        if (!token) return;

        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/templates`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        setTemplates(response.data);
      } catch (err: any) {
        console.error("Failed to load templates", err);
        setError(language === "ne" ? "ढाँचाहरू लोड गर्न सकिएन।" : "Failed to load templates.");
      } finally {
        setLoading(false);
      }
    }
    fetchTemplates();
  }, [getToken, language]);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center gap-4 text-foreground">
        <div className="relative">
          <Loader2 className="h-10 w-10 text-primary animate-spin" />
          <Sparkles className="absolute -top-1 -right-1 w-4 h-4 text-primary/60 animate-pulse" />
        </div>
        <p className="font-serif tracking-wide text-muted-foreground">{t.loading}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-20 border-b border-border bg-background/90 backdrop-blur-xl px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/dashboard" className="flex items-center gap-2 group">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
              <Scale className="w-4.5 h-4.5 text-primary-foreground" />
            </div>
            <span className="text-lg font-serif font-bold text-foreground">LegalEase</span>
          </Link>
          <span className="text-muted-foreground/30 text-lg">/</span>
          <span className="text-sm font-semibold text-muted-foreground">{t.templates}</span>
        </div>
        <div className="flex items-center gap-3">
          <LanguageToggler />
          <Link
            href="/dashboard"
            className="text-xs font-semibold bg-muted hover:bg-muted/80 border border-border px-4 py-2 rounded-lg transition-all"
          >
            {language === "ne" ? "ड्यासबोर्ड" : "Dashboard"}
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className="px-6 py-12 max-w-6xl mx-auto text-center space-y-4">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-xs font-semibold text-primary">
          <Sparkles className="w-3.5 h-3.5 animate-pulse" />
          {language === "ne" ? "स्मार्ट एआई ड्राफ्टिङ" : "Smart AI Drafting"}
        </div>
        <h1 className="text-4xl md:text-5xl font-serif font-bold tracking-tight text-foreground leading-tight">
          {t.templatesTitle}
        </h1>
        <p className="text-muted-foreground text-sm max-w-2xl mx-auto font-light leading-relaxed">
          {t.templatesSub}
        </p>
      </section>

      {/* Main Grid */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-6 pb-16">
        {error ? (
          <div className="bg-red-500/10 border border-red-500/20 text-destructive rounded-xl p-6 text-center max-w-md mx-auto">
            <HelpCircle className="w-8 h-8 mx-auto mb-2" />
            <p className="text-sm font-semibold">{error}</p>
          </div>
        ) : templates.length === 0 ? (
          <div className="text-center py-16 rounded-2xl border border-dashed border-border text-muted-foreground max-w-md mx-auto">
            <BookOpen className="w-10 h-10 mx-auto mb-3 opacity-25" />
            <p className="text-sm">{language === "ne" ? "कुनै ढाँचा फेला परेन।" : "No templates found."}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-6">
            {templates.map((template) => {
              // Parse field count
              let fieldCount = 0;
              try {
                fieldCount = JSON.parse(template.variables).length;
              } catch (e) {}

              return (
                <div
                  key={template.id}
                  className="group bg-card border border-border rounded-2xl overflow-hidden hover:border-primary/40 hover:shadow-lg transition-all duration-300 flex flex-col justify-between"
                >
                  <div className="p-6 space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-3xl" role="img" aria-label="icon">
                        {categoryIcons[template.category as keyof typeof categoryIcons] || "📄"}
                      </span>
                      <span className="text-[10px] font-mono font-bold bg-muted text-muted-foreground px-2 py-0.5 rounded-full border border-border">
                        {fieldCount} {language === "ne" ? "क्षेत्रहरू (Fields)" : "Fields"}
                      </span>
                    </div>

                    <div className="space-y-1.5">
                      <h2 className="text-lg font-serif font-bold text-foreground leading-snug group-hover:text-primary transition-colors">
                        {template.title}
                      </h2>
                      <p className="text-xs text-muted-foreground font-light leading-relaxed">
                        {template.category === "RENTAL" && (language === "ne" ? "नेपालको प्रचलित कानून बमोजिम घर, कोठा वा फ्ल्याट बहालमा दिँदा गरिने घर बहाल सम्झौता ढाँचा।" : "Standard rental contract complying with Nepal rent laws for residential/commercial properties.")}
                        {template.category === "EMPLOYMENT" && (language === "ne" ? "श्रम ऐन २०७४ अनुसार रोजगारदाता र कर्मचारी बीच गरिने मानक रोजगारी सम्झौता पत्र।" : "Standard employment agreement fully compliant with Nepal Labor Act 2074.")}
                        {template.category === "NDA" && (language === "ne" ? "व्यवसायिक साझेदारी वा परियोजनाहरूमा गोप्य सूचनाहरू सुरक्षित राख्न गरिने सम्झौता।" : "Mutual non-disclosure agreement to secure confidential project/business information.")}
                        {template.category === "LOAN" && (language === "ne" ? "व्यक्तिगत वा संस्थागत लेनदेन तथा ऋण लगानी गर्दा गरिने कपाली तमसुक वा सम्झौता पत्र।" : "Standard loan contract or Promissory Note (Kapali Tamasuk) for financial lending.")}
                      </p>
                    </div>
                  </div>

                  <div className="px-6 py-4 bg-muted/20 border-t border-border/50 flex items-center justify-between">
                    <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
                      <ShieldCheck className="w-3.5 h-3.5 text-primary" />
                      <span>{language === "ne" ? "कानूनी रूपमा मान्य" : "Legally Compliant"}</span>
                    </div>
                    <Link
                      href={`/templates/${template.slug}`}
                      className="inline-flex items-center gap-1.5 text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-xl transition-all shadow-md shadow-primary/20 group-hover:shadow-primary/30 active:scale-95"
                    >
                      <span>{language === "ne" ? "ड्राफ्ट सुरु गर्नुहोस्" : "Start Drafting"}</span>
                      <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
}
