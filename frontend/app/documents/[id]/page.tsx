"use client";

import React, { useState, useEffect } from "react";
import { useAuth } from "@clerk/nextjs";
import axios from "axios";
import { FileText, Loader2, ArrowRight, ShieldAlert, ShieldCheck, Shield, MessageSquare } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSelector } from "react-redux";
import { RootState } from "../../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

interface KeyClause {
  text: string;
  risk: "SAFE" | "REVIEW" | "DANGER";
  explanation: string;
}

interface DocAnalysis {
  id: string;
  summary: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  keyClauses: string;
}

interface Document {
  id: string;
  fileName: string;
  fileUrl: string;
  status: "PENDING" | "PROCESSING" | "DONE" | "FAILED";
  language: string;
  createdAt: string;
  analysis?: DocAnalysis;
}

export default function DocumentDetails() {
  const { id } = useParams();
  const { getToken } = useAuth();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];
  const [doc, setDoc] = useState<Document | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchDocDetails() {
      try {
        const token = await getToken();
        if (!token) return;

        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents/${id}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        setDoc(response.data);
      } catch (err: any) {
        console.error("Failed to load document details", err);
        setError("कागजातको विवरण लोड गर्न सकिएन।");
      } finally {
        setLoading(false);
      }
    }
    fetchDocDetails();
  }, [id, getToken]);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-foreground">
        <Loader2 className="h-10 w-10 text-primary animate-spin mb-4" />
        <p>लोड हुँदैछ (Loading)...</p>
      </div>
    );
  }

  if (error || !doc) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-foreground px-6 text-center space-y-4">
        <ShieldAlert className="h-12 w-12 text-red-500" />
        <h2 className="text-2xl font-bold">{error || "कागजात फेला परेन"}</h2>
        <Link href="/dashboard" className="bg-primary hover:bg-primary/90 text-primary-foreground px-6 py-2.5 rounded-md font-semibold">
          ड्यासबोर्डमा फर्कनुहोस्
        </Link>
      </div>
    );
  }

  // Parse key clauses JSON string
  let clauses: KeyClause[] = [];
  if (doc.analysis?.keyClauses) {
    try {
      clauses = JSON.parse(doc.analysis.keyClauses);
    } catch (e) {
      console.error("Failed to parse key clauses", e);
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      {/* Header bar */}
      <header className="border-b border-border bg-card px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/dashboard" className="text-sm text-muted-foreground hover:text-primary">
            {t.backToDashboard}
          </Link>
          <span className="text-muted-foreground/20">/</span>
          <h2 className="font-serif font-bold text-lg truncate max-w-[280px]" title={doc.fileName}>
            {doc.fileName}
          </h2>
        </div>

        <div className="flex items-center gap-4">
          <LanguageToggler />
          <a
            href={doc.fileUrl}
            target="_blank"
            rel="noreferrer"
            className="bg-muted hover:bg-muted/85 border border-border px-4 py-2 rounded-md text-xs font-semibold"
          >
            {t.openOriginal}
          </a>
        </div>
      </header>

      {/* Details layout grids */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-2">
        {/* Left Side: Summary and General Details */}
        <div className="p-8 border-r border-border space-y-8 overflow-y-auto max-h-[calc(100vh-70px)]">
          <div className="space-y-4">
            <h3 className="text-2xl font-serif font-bold text-primary">{t.docSummary}</h3>
            <div className="bg-card border border-border p-6 rounded-xl leading-relaxed text-left text-base space-y-4">
              {doc.analysis?.summary.split("\n").map((para, index) => (
                <p key={index}>{para}</p>
              )) || <p>{t.noSummary}</p>}
            </div>
          </div>

          {/* Quick Stats Panel */}
          <div className="bg-muted border border-border p-6 rounded-xl grid grid-cols-2 gap-4">
            <div className="text-left">
              <span className="text-xs text-muted-foreground block">{t.riskStatus}</span>
              <span
                className={`font-bold mt-1 inline-block ${
                  doc.analysis?.riskLevel === "HIGH"
                    ? "text-red-500"
                    : doc.analysis?.riskLevel === "MEDIUM"
                    ? "text-yellow-500"
                    : "text-green-500"
                }`}
              >
                {doc.analysis?.riskLevel || "LOW"}
              </span>
            </div>

            <div className="text-left">
              <span className="text-xs text-muted-foreground block">{t.uploadedAt}</span>
              <span className="font-semibold text-sm mt-1 inline-block">
                {new Date(doc.createdAt).toLocaleDateString(language === "ne" ? "ne-NP" : "en-US")}
              </span>
            </div>
          </div>
        </div>

        {/* Right Side: Key Clauses & Risk Indicators */}
        <div className="p-8 space-y-8 overflow-y-auto max-h-[calc(100vh-70px)]">
          <div className="space-y-4">
            <h3 className="text-2xl font-serif font-bold text-foreground">{t.keyClauses}</h3>

            {clauses.length === 0 ? (
              <div className="text-center py-12 bg-card/30 border border-border rounded-xl text-muted-foreground">
                <p>{t.noClauses}</p>
              </div>
            ) : (
              <div className="space-y-4">
                {clauses.map((clause, index) => (
                  <div
                    key={index}
                    className="bg-card border border-border p-5 rounded-xl space-y-3 text-left"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <h4 className="font-bold text-sm text-muted-foreground">बुँदा #{index + 1}</h4>
                      <span
                        className={`text-xs px-2.5 py-0.5 rounded font-bold flex items-center gap-1 ${
                          clause.risk === "DANGER"
                            ? "bg-red-500/10 text-red-400"
                            : clause.risk === "REVIEW"
                            ? "bg-yellow-500/10 text-yellow-400"
                            : "bg-green-500/10 text-green-400"
                        }`}
                      >
                        {clause.risk === "DANGER" && <ShieldAlert className="h-3.5 w-3.5" />}
                        {clause.risk === "REVIEW" && <Shield className="h-3.5 w-3.5" />}
                        {clause.risk === "SAFE" && <ShieldCheck className="h-3.5 w-3.5" />}
                        {clause.risk}
                      </span>
                    </div>

                    <blockquote className="border-l-2 border-primary pl-3 text-sm italic text-foreground/80 leading-relaxed">
                      "{clause.text}"
                    </blockquote>

                    <p className="text-sm leading-relaxed pt-1 text-foreground font-light">
                      {clause.explanation}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Placeholder for AI chat integration (Phase 2 preview) */}
          <div className="bg-card border border-border p-6 rounded-xl flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <MessageSquare className="h-6 w-6 text-primary" />
              <div className="text-left">
                <h4 className="font-bold">{t.chatbotTitle}</h4>
                <p className="text-xs text-muted-foreground">{t.chatbotSub}</p>
              </div>
            </div>
            <button className="bg-muted text-foreground/45 border border-border px-4 py-2 rounded-md text-xs font-semibold cursor-not-allowed">
              {t.chatNow}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
