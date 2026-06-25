"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useAuth, useUser, UserButton } from "@clerk/nextjs";
import { useDropzone } from "react-dropzone";
import axios from "axios";
import { FileUp, Loader2, FileText, CheckCircle2, AlertTriangle, AlertCircle } from "lucide-react";
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

export default function Dashboard() {
  const { getToken } = useAuth();
  const { user } = useUser();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(true);
  
  // Upload & processing states
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState("");
  const [uploadError, setUploadError] = useState<string | null>(null);

  const fetchDocuments = useCallback(async () => {
    try {
      const token = await getToken();
      if (!token) return;

      const response = await axios.get(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setDocuments(response.data);
    } catch (err) {
      console.error("Failed to fetch documents", err);
    } finally {
      setLoadingDocs(false);
    }
  }, [getToken]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  // Polling for processing files
  useEffect(() => {
    const hasProcessingDocs = documents.some(
      (doc) => doc.status === "PENDING" || doc.status === "PROCESSING"
    );

    if (hasProcessingDocs) {
      const interval = setInterval(() => {
        fetchDocuments();
      }, 4000);
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
            headers: {
              Authorization: `Bearer ${token}`,
              "Content-Type": "multipart/form-data",
            },
            params: {
              email: user?.primaryEmailAddress?.emailAddress,
              name: user?.fullName,
            },
          }
        );

        setDocuments((prev) => [response.data, ...prev]);
        setIsUploading(false);
      } catch (err: any) {
        console.error("Upload failed", err);
        setUploadError(t.uploadError);
        setIsUploading(false);
      }
    },
    [getToken, user]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/pdf": [".pdf"],
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document": [".docx"],
    },
    maxFiles: 1,
    disabled: isUploading,
  });

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* Sidebar navigation */}
      <aside className="w-64 border-r border-border bg-card p-6 flex flex-col justify-between hidden md:flex">
        <div className="space-y-8">
          <div className="flex items-center justify-between">
            <span className="text-xl font-serif font-bold text-primary">⚖️ LegalEase</span>
            <LanguageToggler />
          </div>

          <nav className="space-y-2">
            <Link
              href="/dashboard"
              className="flex items-center gap-3 px-4 py-2.5 rounded-lg bg-muted text-foreground font-medium border border-border"
            >
              📊 {t.dashboard}
            </Link>
            <Link
              href="/templates"
              className="flex items-center gap-3 px-4 py-2.5 rounded-lg text-foreground/80 hover:bg-muted hover:text-foreground transition-all"
            >
              {t.templates}
            </Link>
            <Link
              href="/lawyers"
              className="flex items-center gap-3 px-4 py-2.5 rounded-lg text-foreground/80 hover:bg-muted hover:text-foreground transition-all"
            >
              {t.findLawyersSidebar}
            </Link>
          </nav>
        </div>

        <div className="flex items-center justify-between border-t border-border pt-4">
          <div className="flex items-center gap-3">
            <UserButton afterSignOutUrl="/" />
            <div className="text-left">
              <p className="text-xs font-semibold">{user?.fullName}</p>
              <p className="text-[10px] text-muted-foreground">{user?.primaryEmailAddress?.emailAddress}</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main dashboard content */}
      <main className="flex-1 p-8 space-y-8">
        <header className="flex items-center justify-between">
          <h2 className="text-3xl font-serif font-bold text-foreground">
            {t.yourDocuments}
          </h2>
          <div className="flex items-center gap-3 md:hidden">
            <LanguageToggler />
            <UserButton afterSignOutUrl="/" />
          </div>
        </header>

        {/* Dropzone file uploader */}
        <section
          {...getRootProps()}
          className={`border-2 border-dashed rounded-xl p-10 text-center transition-all cursor-pointer ${
            isDragActive
              ? "border-primary bg-primary/5"
              : "border-border hover:border-muted bg-card/50"
          } ${isUploading ? "opacity-60 cursor-not-allowed" : ""}`}
        >
          <input {...getInputProps()} />
          <div className="flex flex-col items-center gap-4">
            {isUploading ? (
              <Loader2 className="h-10 w-10 text-primary animate-spin" />
            ) : (
              <FileUp className="h-10 w-10 text-muted-foreground" />
            )}
            <div>
              <p className="font-semibold text-lg">
                {isDragActive
                  ? t.dropActive
                  : t.dropPlaceholder}
              </p>
              <p className="text-sm text-muted-foreground mt-1">{t.fileLimits}</p>
            </div>

            {uploadProgress && <p className="text-sm text-primary font-medium">{uploadProgress}</p>}
            {uploadError && (
              <div className="flex items-center gap-2 text-red-500 text-sm font-semibold">
                <AlertCircle className="h-4 w-4" />
                <span>{uploadError}</span>
              </div>
            )}
          </div>
        </section>

        {/* Documents List Grid */}
        <section className="space-y-4">
          <h3 className="text-xl font-serif font-bold text-foreground">{t.uploadedFiles}</h3>

          {loadingDocs ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-8 w-8 text-primary animate-spin" />
            </div>
          ) : documents.length === 0 ? (
            <div className="text-center py-16 bg-card/30 border border-border rounded-xl text-muted-foreground">
              <FileText className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p>{t.noDocuments}</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {documents.map((doc) => (
                <div
                  key={doc.id}
                  className="bg-card border border-border hover:border-muted p-6 rounded-xl space-y-4 transition-all relative flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <FileText className="h-8 w-8 text-primary/80 flex-shrink-0" />
                        <h4 className="font-semibold text-lg text-foreground truncate max-w-[200px]" title={doc.fileName}>
                          {doc.fileName}
                        </h4>
                      </div>
                      <span
                        className={`text-xs px-2.5 py-1 rounded-full font-medium ${
                          doc.status === "DONE"
                            ? "bg-green-500/10 text-green-400 border border-green-500/20"
                            : doc.status === "FAILED"
                            ? "bg-red-500/10 text-red-400 border border-red-500/20"
                            : "bg-primary/10 text-primary border border-primary/20 animate-pulse"
                        }`}
                      >
                        {doc.status === "DONE"
                          ? t.statusDone
                          : doc.status === "FAILED"
                          ? t.statusFailed
                          : t.statusProcessing}
                      </span>
                    </div>

                    {/* Quick summary snippet if available */}
                    {doc.analysis && (
                      <div className="mt-4 space-y-3">
                        <div className="flex items-center gap-2">
                          <span className="text-xs text-muted-foreground font-semibold">{t.riskLevel}:</span>
                          <span
                            className={`text-xs font-bold px-2 py-0.5 rounded flex items-center gap-1 ${
                              doc.analysis.riskLevel === "HIGH"
                                ? "bg-red-500/10 text-red-500"
                                : doc.analysis.riskLevel === "MEDIUM"
                                ? "bg-yellow-500/10 text-yellow-500"
                                : "bg-green-500/10 text-green-500"
                            }`}
                          >
                            {doc.analysis.riskLevel === "HIGH" && <AlertTriangle className="h-3 w-3" />}
                            {doc.analysis.riskLevel}
                          </span>
                        </div>
                        <p className="text-sm text-foreground/80 line-clamp-3 leading-relaxed">
                          {doc.analysis.summary}
                        </p>
                      </div>
                    )}
                  </div>

                  {doc.status === "DONE" && (
                    <div className="pt-4 border-t border-border mt-4 flex items-center justify-between">
                      <a
                        href={doc.fileUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-muted-foreground hover:text-primary transition-colors"
                      >
                        {t.download}
                      </a>
                      <Link
                        href={`/documents/${doc.id}`}
                        className="text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-3 py-1.5 rounded-md transition-all cursor-pointer"
                      >
                        {t.viewDetails}
                      </Link>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
