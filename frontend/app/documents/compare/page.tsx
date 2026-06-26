"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@clerk/nextjs";
import axios from "axios";
import {
  FileText, Loader2, Sparkles, Scale, ArrowLeft,
  ArrowRight, ShieldAlert, GitCompare, ChevronDown
} from "lucide-react";
import Link from "next/link";
import { useSelector } from "react-redux";
import { RootState } from "../../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

interface DocumentItem {
  id: string;
  fileName: string;
  status: string;
}

interface ChangeItem {
  type: "ADDED" | "DELETED" | "MODIFIED" | "SUSPICIOUS";
  clauseTitle: string;
  originalText: string;
  modifiedText: string;
  explanation: string;
}

interface ComparisonResult {
  overallSummary: string;
  changes: ChangeItem[];
}

const typeColors = {
  ADDED: { text: "text-green-500", bg: "bg-green-500/10", border: "border-green-500/20", animate: "" },
  DELETED: { text: "text-red-500", bg: "bg-red-500/10", border: "border-red-500/20", animate: "" },
  MODIFIED: { text: "text-blue-500", bg: "bg-blue-500/10", border: "border-blue-500/20", animate: "" },
  SUSPICIOUS: { text: "text-yellow-500", bg: "bg-yellow-500/10", border: "border-yellow-500/20", animate: "animate-pulse" },
};

export default function DocumentComparison() {
  const { getToken } = useAuth();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [docsLoading, setDocsLoading] = useState(true);

  const [baseDocId, setBaseDocId] = useState("");
  const [compareDocId, setCompareDocId] = useState("");

  const [comparing, setComparing] = useState(false);
  const [result, setResult] = useState<ComparisonResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchUserDocs() {
      try {
        const token = await getToken();
        if (!token) return;

        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        // Only allow completed documents to be compared
        const completedDocs = response.data.filter((d: any) => d.status === "DONE");
        setDocuments(completedDocs);
      } catch (err) {
        console.error("Failed to load documents for comparison", err);
      } finally {
        setDocsLoading(false);
      }
    }
    fetchUserDocs();
  }, [getToken]);

  const handleCompare = async () => {
    if (!baseDocId || !compareDocId) return;
    if (baseDocId === compareDocId) {
      setError(language === "ne" ? "कृपया दुई फरक कागजातहरू चयन गर्नुहोस्।" : "Please select two different documents.");
      return;
    }

    setComparing(true);
    setResult(null);
    setError(null);

    try {
      const token = await getToken();
      if (!token) return;

      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents/compare`,
        {
          baseDocId,
          compareDocId
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      const parsedResult = JSON.parse(response.data.comparisonReport) as ComparisonResult;
      setResult(parsedResult);
    } catch (err) {
      console.error("Failed to run document comparison", err);
      setError(language === "ne" ? "तुलना विश्लेषण असफल भयो।" : "Comparison analysis failed.");
    } finally {
      setComparing(false);
    }
  };

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
          <span className="text-sm font-semibold text-muted-foreground">{t.compareDocs}</span>
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

      {/* Selectors Panel */}
      <main className="flex-1 max-w-5xl w-full mx-auto px-6 py-10 space-y-8">
        <section className="bg-card border border-border rounded-2xl p-6 shadow-sm space-y-6">
          <div className="space-y-1.5 text-center sm:text-left">
            <h2 className="text-xl font-serif font-bold text-foreground flex items-center justify-center sm:justify-start gap-2">
              <GitCompare className="w-5 h-5 text-primary animate-pulse" />
              {t.compareTitle}
            </h2>
            <p className="text-xs text-muted-foreground font-light">{t.selectDocs}</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground/80">{t.baseDocLabel}</label>
              <div className="relative">
                <select
                  value={baseDocId}
                  onChange={(e) => setBaseDocId(e.target.value)}
                  disabled={docsLoading || comparing}
                  className="w-full appearance-none border border-border bg-background rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all text-foreground/90 disabled:opacity-40"
                >
                  <option value="">{language === "ne" ? "-- पहिलो कागजात चयन गर्नुहोस् --" : "-- Select Base Document --"}</option>
                  {documents.map((d) => (
                    <option key={d.id} value={d.id}>{d.fileName}</option>
                  ))}
                </select>
                <ChevronDown className="absolute right-4 top-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-foreground/80">{t.compareDocLabel}</label>
              <div className="relative">
                <select
                  value={compareDocId}
                  onChange={(e) => setCompareDocId(e.target.value)}
                  disabled={docsLoading || comparing}
                  className="w-full appearance-none border border-border bg-background rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all text-foreground/90 disabled:opacity-40"
                >
                  <option value="">{language === "ne" ? "-- दोस्रो कागजात चयन गर्नुहोस् --" : "-- Select Comparison Document --"}</option>
                  {documents.map((d) => (
                    <option key={d.id} value={d.id}>{d.fileName}</option>
                  ))}
                </select>
                <ChevronDown className="absolute right-4 top-3.5 w-4 h-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>
          </div>

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 text-destructive text-xs font-semibold px-4 py-3 rounded-xl">
              {error}
            </div>
          )}

          <div className="flex justify-center sm:justify-end">
            <button
              onClick={handleCompare}
              disabled={comparing || !baseDocId || !compareDocId}
              className="inline-flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground font-semibold px-6 py-3 rounded-xl transition-all shadow-md shadow-primary/25 disabled:opacity-40 disabled:cursor-not-allowed hover:shadow-primary/35"
            >
              {comparing ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>{t.comparing}</span>
                </>
              ) : (
                <>
                  <GitCompare className="w-4 h-4" />
                  <span>{t.runComparison}</span>
                </>
              )}
            </button>
          </div>
        </section>

        {/* Comparison Result Panel */}
        {result && (
          <section className="space-y-6 animate-fade-in">
            {/* Overall Summary Card */}
            <div className="bg-card border border-border rounded-2xl p-6 shadow-sm space-y-3">
              <h3 className="text-base font-serif font-bold text-foreground flex items-center gap-2 border-b border-border/50 pb-3">
                <Sparkles className="w-4 h-4 text-primary" />
                {t.comparisonSummary}
              </h3>
              <p className="text-sm text-foreground/85 leading-relaxed font-light font-sans">
                {result.overallSummary}
              </p>
            </div>

            {/* Changes Checklist */}
            <div className="space-y-4">
              {result.changes.length === 0 ? (
                <div className="bg-card border border-border rounded-2xl p-8 text-center text-muted-foreground">
                  {t.noChanges}
                </div>
              ) : (
                result.changes.map((item, index) => {
                  const tc = typeColors[item.type];
                  return (
                    <div
                      key={index}
                      className="bg-card border border-border rounded-2xl overflow-hidden hover:border-border/60 hover:shadow-md transition-all space-y-3 p-5"
                    >
                      <div className="flex items-center justify-between">
                        <h4 className="font-serif font-bold text-sm text-foreground">{item.clauseTitle}</h4>
                        <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full border ${tc.bg} ${tc.text} ${tc.border} ${tc.animate || ""}`}>
                          {item.type === "SUSPICIOUS" && <ShieldAlert className="w-3 h-3" />}
                          {item.type === "ADDED" && t.typeAdded}
                          {item.type === "DELETED" && t.typeDeleted}
                          {item.type === "MODIFIED" && t.typeModified}
                          {item.type === "SUSPICIOUS" && t.typeSuspicious}
                        </span>
                      </div>

                      {/* Side-by-Side Clause Text Comparison */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono py-2">
                        {item.type !== "ADDED" && (
                          <div className="bg-red-500/3 border border-red-500/10 rounded-xl p-3 space-y-1.5">
                            <span className="text-[9px] font-bold uppercase tracking-wider text-red-500/80">{t.originalTextLabel}</span>
                            <p className="text-foreground/80 leading-relaxed italic">"{item.originalText}"</p>
                          </div>
                        )}
                        {item.type !== "DELETED" && (
                          <div className="bg-green-500/3 border border-green-500/10 rounded-xl p-3 space-y-1.5">
                            <span className="text-[9px] font-bold uppercase tracking-wider text-green-500/80">{t.modifiedTextLabel}</span>
                            <p className="text-foreground/80 leading-relaxed italic">"{item.modifiedText}"</p>
                          </div>
                        )}
                      </div>

                      {/* AI Explanation */}
                      <div className="bg-muted/30 border border-border/40 rounded-xl p-4 text-xs leading-relaxed text-foreground/75 font-sans font-light">
                        <span className="font-semibold text-foreground/85 block mb-1">{t.explanation}:</span>
                        {item.explanation}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
