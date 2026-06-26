"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useAuth, useUser, UserButton } from "@clerk/nextjs";
import { useDropzone } from "react-dropzone";
import axios from "axios";
import {
  FileUp, Loader2, FileText, CheckCircle2, AlertTriangle,
  AlertCircle, LayoutDashboard, FileStack, Users, ChevronRight,
  Clock, TrendingUp, Scale, Sparkles
} from "lucide-react";
import Link from "next/link";
import { useSelector } from "react-redux";
import { RootState } from "../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

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

const navItems = [
  { href: "/dashboard", icon: LayoutDashboard, labelKey: "dashboard" },
  { href: "/templates", icon: FileStack, labelKey: "templates" },
  { href: "/lawyers", icon: Users, labelKey: "findLawyersSidebar" },
];

const riskConfig = {
  HIGH: { color: "text-red-500", bg: "bg-red-500/10", border: "border-red-500/20", dot: "bg-red-500" },
  MEDIUM: { color: "text-yellow-500", bg: "bg-yellow-500/10", border: "border-yellow-500/20", dot: "bg-yellow-500" },
  LOW: { color: "text-green-500", bg: "bg-green-500/10", border: "border-green-500/20", dot: "bg-green-500" },
};

export default function Dashboard() {
  const { getToken } = useAuth();
  const { user } = useUser();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState("");
  const [uploadError, setUploadError] = useState<string | null>(null);

  const fetchDocuments = useCallback(async () => {
    try {
      const token = await getToken();
      if (!token) return;
      const response = await axios.get(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setDocuments(response.data);
    } catch (err) {
      console.error("Failed to fetch documents", err);
    } finally {
      setLoadingDocs(false);
    }
  }, [getToken]);

  useEffect(() => { fetchDocuments(); }, [fetchDocuments]);

  useEffect(() => {
    const hasProcessingDocs = documents.some(
      (doc) => doc.status === "PENDING" || doc.status === "PROCESSING"
    );
    if (hasProcessingDocs) {
      const interval = setInterval(() => { fetchDocuments(); }, 4000);
      return () => clearInterval(interval);
    }
  }, [documents, fetchDocuments]);

  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      if (acceptedFiles.length === 0) return;
      const file = acceptedFiles[0];
      setIsUploading(true);
      setUploadError(null);
      setUploadProgress(t.uploading);
      try {
        const token = await getToken();
        if (!token) throw new Error("No authentication token found");
        const formData = new FormData();
        formData.append("file", file);
        setUploadProgress(t.analyzing);
        const response = await axios.post(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents/upload`,
          formData,
          {
            headers: { Authorization: `Bearer ${token}`, "Content-Type": "multipart/form-data" },
            params: { email: user?.primaryEmailAddress?.emailAddress, name: user?.fullName },
          }
        );
        setDocuments((prev) => [response.data, ...prev]);
        setIsUploading(false);
        setUploadProgress("");
      } catch (err: any) {
        console.error("Upload failed", err);
        setUploadError(t.uploadError);
        setIsUploading(false);
        setUploadProgress("");
      }
    },
    [getToken, user, t]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/pdf": [".pdf"],
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document": [".docx"],
      "image/*": [".png", ".jpg", ".jpeg", ".webp"],
    },
    maxFiles: 1,
    disabled: isUploading,
  });

  const doneCount = documents.filter((d) => d.status === "DONE").length;
  const processingCount = documents.filter(
    (d) => d.status === "PENDING" || d.status === "PROCESSING"
  ).length;
  const highRiskCount = documents.filter(
    (d) => d.analysis?.riskLevel === "HIGH"
  ).length;

  return (
    <div className="flex min-h-screen bg-background text-foreground">

      {/* ── Sidebar ─────────────────────────────────────────────────── */}
      <aside className="w-64 shrink-0 border-r border-border bg-card hidden md:flex flex-col justify-between">
        <div className="p-6 space-y-8">
          {/* Brand */}
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center shadow-lg">
              <Scale className="w-4 h-4 text-primary-foreground" />
            </div>
            <span className="text-lg font-serif font-bold text-foreground">LegalEase</span>
          </div>

          {/* Nav */}
          <nav className="space-y-1">
            {navItems.map(({ href, icon: Icon, labelKey }) => {
              const isActive = href === "/dashboard";
              return (
                <Link
                  key={href}
                  href={href}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all group ${
                    isActive
                      ? "bg-primary text-primary-foreground shadow-md shadow-primary/25"
                      : "text-muted-foreground hover:text-foreground hover:bg-muted"
                  }`}
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{(t as any)[labelKey]?.replace(/^📋\s*|^👨‍⚖️\s*/, "")}</span>
                  {isActive && <ChevronRight className="w-3.5 h-3.5 ml-auto" />}
                </Link>
              );
            })}
          </nav>

          {/* Quick stats in sidebar */}
          <div className="rounded-xl border border-border bg-muted/30 p-4 space-y-3">
            <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
              Overview
            </p>
            <div className="space-y-2">
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground">Analyzed</span>
                <span className="font-semibold text-foreground">{doneCount}</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground">Processing</span>
                <span className={`font-semibold ${processingCount > 0 ? "text-primary animate-pulse" : "text-foreground"}`}>
                  {processingCount}
                </span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-muted-foreground">High Risk</span>
                <span className={`font-semibold ${highRiskCount > 0 ? "text-red-500" : "text-foreground"}`}>
                  {highRiskCount}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* User footer */}
        <div className="p-4 border-t border-border flex items-center gap-3">
          <UserButton afterSignOutUrl="/" />
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-foreground truncate">{user?.fullName}</p>
            <p className="text-[10px] text-muted-foreground truncate">
              {user?.primaryEmailAddress?.emailAddress}
            </p>
          </div>
          <LanguageToggler />
        </div>
      </aside>

      {/* ── Main Content ─────────────────────────────────────────────── */}
      <main className="flex-1 overflow-auto">
        {/* Top bar (mobile) */}
        <header className="border-b border-border px-6 py-4 flex items-center justify-between md:px-8 bg-background/80 backdrop-blur sticky top-0 z-10">
          <div className="flex items-center gap-3">
            <Scale className="w-5 h-5 text-primary md:hidden" />
            <h1 className="text-xl font-serif font-bold text-foreground">{t.yourDocuments}</h1>
          </div>
          <div className="flex items-center gap-3 md:hidden">
            <LanguageToggler />
            <UserButton afterSignOutUrl="/" />
          </div>
        </header>

        <div className="p-6 md:p-8 space-y-8">

          {/* ── Metric Cards ── */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: "Total Documents", value: documents.length, icon: FileText, color: "text-primary" },
              { label: "Analyzed", value: doneCount, icon: CheckCircle2, color: "text-green-500" },
              { label: "Processing", value: processingCount, icon: TrendingUp, color: "text-primary" },
              { label: "High Risk", value: highRiskCount, icon: AlertTriangle, color: "text-red-500" },
            ].map(({ label, value, icon: Icon, color }, i) => (
              <div key={i} className="bg-card border border-border rounded-xl p-5 flex items-start gap-3 hover:border-border/60 hover:shadow-md transition-all">
                <div className="w-9 h-9 rounded-lg bg-muted flex items-center justify-center shrink-0">
                  <Icon className={`w-4.5 h-4.5 ${color}`} />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground leading-none">{value}</p>
                  <p className="text-[11px] text-muted-foreground mt-1">{label}</p>
                </div>
              </div>
            ))}
          </div>

          {/* ── Upload Zone ── */}
          <div
            {...getRootProps()}
            className={`relative overflow-hidden rounded-2xl border-2 border-dashed p-10 text-center transition-all cursor-pointer group ${
              isDragActive
                ? "border-primary bg-primary/5 scale-[1.01]"
                : "border-border hover:border-primary/50 bg-card/50 hover:bg-primary/3"
            } ${isUploading ? "opacity-60 cursor-not-allowed" : ""}`}
          >
            <input {...getInputProps()} />

            {/* Subtle animated bg on hover */}
            <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"
              style={{
                background: "radial-gradient(ellipse at 50% 120%, oklch(0.555 0.163 49 / 0.06) 0%, transparent 70%)"
              }}
            />

            <div className="relative flex flex-col items-center gap-4">
              {isUploading ? (
                <div className="relative">
                  <Loader2 className="h-12 w-12 text-primary animate-spin" />
                  <Sparkles className="absolute -top-1 -right-1 w-4 h-4 text-primary/60 animate-pulse" />
                </div>
              ) : (
                <div className={`w-16 h-16 rounded-2xl flex items-center justify-center transition-all duration-300 ${
                  isDragActive ? "bg-primary/20 scale-110" : "bg-muted group-hover:bg-primary/10"
                }`}>
                  <FileUp className={`h-7 w-7 transition-colors ${isDragActive ? "text-primary" : "text-muted-foreground group-hover:text-primary"}`} />
                </div>
              )}

              <div>
                <p className="font-semibold text-lg text-foreground">
                  {isDragActive ? t.dropActive : t.dropPlaceholder}
                </p>
                <p className="text-sm text-muted-foreground mt-1.5">{t.fileLimits}</p>
              </div>

              {uploadProgress && (
                <div className="flex items-center gap-2 text-sm text-primary font-medium bg-primary/10 border border-primary/20 rounded-lg px-4 py-2">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  {uploadProgress}
                </div>
              )}

              {uploadError && (
                <div className="flex items-center gap-2 text-red-500 text-sm font-semibold bg-red-500/10 border border-red-500/20 rounded-lg px-4 py-2">
                  <AlertCircle className="h-4 w-4" />
                  {uploadError}
                </div>
              )}
            </div>
          </div>

          {/* ── Document Grid ── */}
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-serif font-bold text-foreground">{t.uploadedFiles}</h2>
              <span className="text-xs text-muted-foreground">{documents.length} files</span>
            </div>

            {loadingDocs ? (
              <div className="flex flex-col items-center justify-center py-20 gap-3 text-muted-foreground">
                <Loader2 className="h-8 w-8 text-primary animate-spin" />
                <p className="text-sm">{t.loading}</p>
              </div>
            ) : documents.length === 0 ? (
              <div className="text-center py-20 rounded-2xl border border-dashed border-border bg-card/30">
                <FileText className="h-12 w-12 mx-auto mb-3 text-muted-foreground/30" />
                <p className="text-muted-foreground text-sm">{t.noDocuments}</p>
                <p className="text-muted-foreground/60 text-xs mt-1">Upload your first legal document above</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                {documents.map((doc) => {
                  const risk = doc.analysis?.riskLevel as keyof typeof riskConfig | undefined;
                  const rc = risk ? riskConfig[risk] : riskConfig.LOW;

                  return (
                    <div
                      key={doc.id}
                      className="group bg-card border border-border rounded-2xl p-6 space-y-4 hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5 transition-all duration-300 flex flex-col justify-between"
                    >
                      <div className="space-y-4">
                        {/* Header */}
                        <div className="flex items-start gap-3">
                          <div className="w-10 h-10 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                            <FileText className="w-5 h-5 text-primary" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <h3
                              className="font-semibold text-sm text-foreground truncate"
                              title={doc.fileName}
                            >
                              {doc.fileName}
                            </h3>
                            <div className="flex items-center gap-1.5 mt-1">
                              <Clock className="w-3 h-3 text-muted-foreground" />
                              <span className="text-[11px] text-muted-foreground">
                                {new Date(doc.createdAt).toLocaleDateString(language === "ne" ? "ne-NP" : "en-US")}
                              </span>
                            </div>
                          </div>
                          <span
                            className={`shrink-0 text-[11px] px-2.5 py-1 rounded-full font-semibold border ${
                              doc.status === "DONE"
                                ? "bg-green-500/10 text-green-500 border-green-500/20"
                                : doc.status === "FAILED"
                                ? "bg-red-500/10 text-red-500 border-red-500/20"
                                : "bg-primary/10 text-primary border-primary/20 animate-pulse"
                            }`}
                          >
                            {doc.status === "DONE" ? t.statusDone : doc.status === "FAILED" ? t.statusFailed : t.statusProcessing}
                          </span>
                        </div>

                        {/* Analysis snippet */}
                        {doc.analysis && (
                          <div className="space-y-3">
                            <div className="flex items-center gap-2">
                              <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border ${rc.bg} ${rc.color} ${rc.border}`}>
                                <span className={`w-1.5 h-1.5 rounded-full ${rc.dot} ${risk === "HIGH" ? "animate-pulse" : ""}`} />
                                {risk} RISK
                              </span>
                            </div>
                            <p className="text-xs text-foreground/70 line-clamp-3 leading-relaxed">
                              {doc.analysis.summary}
                            </p>
                          </div>
                        )}
                      </div>

                      {/* Footer actions */}
                      {doc.status === "DONE" && (
                        <div className="pt-4 border-t border-border flex items-center justify-between gap-2">
                          <a
                            href={doc.fileUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-xs text-muted-foreground hover:text-primary transition-colors flex items-center gap-1"
                          >
                            {t.download}
                          </a>
                          <Link
                            href={`/documents/${doc.id}`}
                            className="group/btn flex items-center gap-1.5 text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2 rounded-lg transition-all shadow-md shadow-primary/20 hover:shadow-primary/40"
                          >
                            {t.viewDetails}
                            <ChevronRight className="w-3 h-3 group-hover/btn:translate-x-0.5 transition-transform" />
                          </Link>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
