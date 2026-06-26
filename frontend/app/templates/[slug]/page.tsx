"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@clerk/nextjs";
import axios from "axios";
import {
  FileText, Loader2, Sparkles, Scale, ArrowLeft,
  ChevronRight, ChevronLeft, Download, Eye, Edit3
} from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useSelector } from "react-redux";
import { RootState } from "../../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

interface TemplateField {
  name: string;
  label: string;
  type: "text" | "number" | "date";
  placeholder: string;
}

interface Template {
  id: string;
  slug: string;
  title: string;
  category: string;
  content: string;
  variables: string; // JSON string representing fields
}

export default function TemplateBuilder() {
  const { slug } = useParams();
  const router = useRouter();
  const { getToken } = useAuth();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [template, setTemplate] = useState<Template | null>(null);
  const [fields, setFields] = useState<TemplateField[]>([]);
  const [formValues, setFormValues] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Wizard steps: 0 = Form inputs, 1 = Verification & Generate, 2 = PDF Preview & Download
  const [currentStep, setCurrentStep] = useState(0);
  const [contractContent, setContractContent] = useState("");
  const [exportingPdf, setExportingPdf] = useState(false);

  useEffect(() => {
    async function fetchTemplateDetails() {
      try {
        const token = await getToken();
        if (!token) return;

        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/templates/${slug}`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        setTemplate(response.data);
        
        // Parse fields
        const parsedFields = JSON.parse(response.data.variables) as TemplateField[];
        setFields(parsedFields);

        // Prepopulate default empty values
        const initialVals: Record<string, string> = {};
        parsedFields.forEach(f => {
          initialVals[f.name] = "";
        });
        setFormValues(initialVals);
      } catch (err: any) {
        console.error("Failed to load template details", err);
        setError(language === "ne" ? "ढाँचाको विवरण लोड गर्न सकिएन।" : "Failed to load template details.");
      } finally {
        setLoading(false);
      }
    }
    fetchTemplateDetails();
  }, [slug, getToken, language]);

  // Replace placeholders for live preview
  const getLivePreview = () => {
    if (!template) return "";
    let content = template.content;
    fields.forEach(f => {
      const val = formValues[f.name];
      const placeholder = `{{${f.name}}}`;
      content = content.replace(new RegExp(placeholder, "g"), val || `[ ${f.label.split(" (")[0]} ]`);
    });
    return content;
  };

  const handleInputChange = (fieldName: string, value: string) => {
    setFormValues(prev => ({
      ...prev,
      [fieldName]: value
    }));
  };

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const token = await getToken();
      if (!token) return;

      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/templates/${slug}/generate`,
        formValues,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setContractContent(response.data.content);
      setCurrentStep(1); // Go to final review step
    } catch (err) {
      console.error("Failed to generate contract", err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!template || !contractContent) return;
    setExportingPdf(true);
    try {
      const token = await getToken();
      if (!token) return;

      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/templates/export`,
        {
          title: template.title,
          content: contractContent
        },
        {
          headers: { Authorization: `Bearer ${token}` },
          responseType: "blob" // Retrieve as binary payload
        }
      );

      // Trigger browser download
      const blob = new Blob([response.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `${slug}-${new Date().getTime()}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (err) {
      console.error("Failed to download PDF", err);
    } finally {
      setExportingPdf(false);
    }
  };

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

  if (error || !template) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-foreground px-6 text-center space-y-4">
        <h2 className="text-2xl font-serif font-bold">{error || "Template not found"}</h2>
        <Link href="/templates" className="flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground px-6 py-2.5 font-semibold rounded-xl">
          <ArrowLeft className="w-4 h-4" />
          {t.backToTemplates}
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-20 border-b border-border bg-background/90 backdrop-blur-xl px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link
            href="/templates"
            className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary transition-colors group"
          >
            <ArrowLeft className="h-4 w-4 group-hover:-translate-x-0.5 transition-transform" />
            <span className="hidden sm:block">{t.backToTemplates}</span>
          </Link>

          <span className="text-muted-foreground/30 text-lg">/</span>

          <div className="flex items-center gap-2">
            <Scale className="w-4 h-4 text-primary" />
            <h1 className="font-serif font-bold text-sm truncate max-w-[200px] md:max-w-md">
              {template.title}
            </h1>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <LanguageToggler />
        </div>
      </header>

      {/* Stepper Status Indicator */}
      <div className="bg-card border-b border-border py-3 px-6 shrink-0 flex justify-center items-center gap-2 sm:gap-6 text-xs font-semibold text-muted-foreground">
        <div className={`flex items-center gap-1.5 ${currentStep === 0 ? "text-primary font-bold" : "text-muted-foreground"}`}>
          <span className={`w-5 h-5 rounded-full flex items-center justify-center border text-[10px] ${currentStep === 0 ? "border-primary bg-primary/10 text-primary" : "border-border"}`}>1</span>
          <span>{t.fillVariables}</span>
        </div>
        <span className="h-px w-8 bg-border hidden sm:block" />
        <div className={`flex items-center gap-1.5 ${currentStep === 1 ? "text-primary font-bold" : "text-muted-foreground"}`}>
          <span className={`w-5 h-5 rounded-full flex items-center justify-center border text-[10px] ${currentStep === 1 ? "border-primary bg-primary/10 text-primary" : "border-border"}`}>2</span>
          <span>{t.previewContract}</span>
        </div>
      </div>

      {/* Main Split Screen */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-2 overflow-hidden" style={{ height: "calc(100vh - 105px)" }}>
        
        {/* LEFT PANEL: Dynamic Form / Actions */}
        <div className="border-r border-border flex flex-col overflow-hidden bg-card p-6 justify-between">
          
          <div className="flex-1 overflow-y-auto space-y-6 max-w-lg w-full mx-auto justify-center flex flex-col">
            {currentStep === 0 ? (
              <>
                <div className="space-y-1.5">
                  <h2 className="text-xl font-serif font-bold text-foreground">
                    {language === "ne" ? "सम्झौता विवरणहरू भर्नुहोस्" : "Enter Contract Details"}
                  </h2>
                  <p className="text-xs text-muted-foreground font-light">
                    {language === "ne"
                      ? "सम्झौता तयार गर्न फारमका सबै क्षेत्रहरू भर्नुहोस्। दायाँ तर्फ परिवर्तनहरू लाइभ देखिनेछन्।"
                      : "Fill in all template fields to draft the document. Changes will preview in real time."}
                  </p>
                </div>

                <div className="space-y-4">
                  {fields.map((field) => (
                    <div key={field.name} className="space-y-1.5">
                      <label className="text-xs font-semibold text-foreground/80">
                        {field.label}
                      </label>
                      <input
                        type={field.type}
                        value={formValues[field.name] || ""}
                        onChange={(e) => handleInputChange(field.name, e.target.value)}
                        placeholder={field.placeholder}
                        className="w-full border border-border bg-background rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all placeholder:text-muted-foreground/45"
                      />
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className="space-y-6 text-center py-8">
                <div className="w-16 h-16 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center mx-auto">
                  <Sparkles className="w-8 h-8 text-primary animate-pulse" />
                </div>
                <div className="space-y-2">
                  <h2 className="text-xl font-serif font-bold text-foreground">
                    {language === "ne" ? "सम्झौता सफलतापूर्वक तयार भयो!" : "Agreement successfully drafted!"}
                  </h2>
                  <p className="text-sm text-muted-foreground font-light max-w-sm mx-auto leading-relaxed">
                    {language === "ne"
                      ? "तपाईंको कानूनी सम्झौता ढाँचा तयार छ। पीडीएफ डाउनलोड गर्न वा विवरण सम्पादन गर्न सक्नुहुन्छ।"
                      : "Your legal agreement has been drafted. You can download it as a PDF or go back to edit details."}
                  </p>
                </div>

                <div className="flex flex-col sm:flex-row gap-3 justify-center pt-4">
                  <button
                    onClick={() => setCurrentStep(0)}
                    className="inline-flex items-center justify-center gap-1.5 text-xs font-semibold border border-border bg-card hover:bg-muted/30 px-5 py-3 rounded-xl transition-all"
                  >
                    <Edit3 className="w-4 h-4" />
                    <span>{language === "ne" ? "सच्याउनुहोस्" : "Edit Values"}</span>
                  </button>
                  <button
                    onClick={handleDownloadPdf}
                    disabled={exportingPdf}
                    className="inline-flex items-center justify-center gap-1.5 text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-5 py-3 rounded-xl transition-all shadow-md shadow-primary/20 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {exportingPdf ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>{t.generatingPdf}</span>
                      </>
                    ) : (
                      <>
                        <Download className="w-4 h-4" />
                        <span>{t.exportPdf}</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Stepper Actions Bar */}
          {currentStep === 0 && (
            <div className="border-t border-border pt-4 mt-6 flex justify-end shrink-0 max-w-lg w-full mx-auto">
              <button
                onClick={handleGenerate}
                disabled={fields.some(f => !formValues[f.name])}
                className="inline-flex items-center gap-1.5 text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-5 py-3 rounded-xl transition-all disabled:opacity-40 disabled:cursor-not-allowed shadow-md shadow-primary/20"
              >
                <span>{language === "ne" ? "कागजात तयार गर्नुहोस्" : "Generate Draft"}</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>

        {/* RIGHT PANEL: Live / Final Preview */}
        <div className="flex flex-col overflow-hidden bg-muted/5 p-6">
          <div className="bg-card border border-border rounded-2xl flex-1 flex flex-col overflow-hidden shadow-sm">
            
            {/* Preview Header */}
            <div className="px-6 py-4 border-b border-border bg-muted/10 shrink-0 flex items-center gap-2">
              <Eye className="w-4 h-4 text-primary" />
              <span className="text-xs font-semibold text-foreground/80">
                {language === "ne" ? "सम्झौताको ड्राफ्ट" : "Contract Draft Preview"}
              </span>
            </div>

            {/* Preview Content */}
            <div className="flex-1 overflow-y-auto p-8 font-serif leading-relaxed text-sm text-foreground/90 whitespace-pre-wrap select-none">
              {currentStep === 0 ? getLivePreview() : contractContent}
            </div>
            
          </div>
        </div>

      </div>
    </div>
  );
}
