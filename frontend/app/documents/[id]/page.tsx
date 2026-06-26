"use client";

import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "@clerk/nextjs";
import axios from "axios";
import {
  FileText, Loader2, Send, ShieldAlert, ShieldCheck, Shield,
  MessageSquare, ArrowLeft, Download, Bot, User, Sparkles,
  Scale, AlertTriangle
} from "lucide-react";
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

interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

const riskColors = {
  HIGH: { text: "text-red-500", bg: "bg-red-500/10", border: "border-red-500/20", dot: "bg-red-500 animate-pulse" },
  MEDIUM: { text: "text-yellow-500", bg: "bg-yellow-500/10", border: "border-yellow-500/20", dot: "bg-yellow-500" },
  LOW: { text: "text-green-500", bg: "bg-green-500/10", border: "border-green-500/20", dot: "bg-green-500" },
};

const clauseRiskColors = {
  DANGER: { text: "text-red-500", bg: "bg-red-500/10", border: "border-red-500/20", bar: "bg-red-500" },
  REVIEW: { text: "text-yellow-500", bg: "bg-yellow-500/10", border: "border-yellow-500/20", bar: "bg-yellow-500" },
  SAFE: { text: "text-green-500", bg: "bg-green-500/10", border: "border-green-500/20", bar: "bg-green-500" },
};

export default function DocumentDetails() {
  const { id } = useParams();
  const { getToken } = useAuth();

  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [doc, setDoc] = useState<Document | null>(null);
  const [docLoading, setDocLoading] = useState(true);
  const [docError, setDocError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"summary" | "clauses" | "compliance">("summary");

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [chatInput, setChatInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);

  // Compliance checker states
  const [complianceReport, setComplianceReport] = useState<any[]>([]);
  const [complianceLoading, setComplianceLoading] = useState(false);
  const [complianceError, setComplianceError] = useState<string | null>(null);

  const fetchComplianceReport = async () => {
    if (complianceReport.length > 0 || complianceLoading) return;
    setComplianceLoading(true);
    setComplianceError(null);
    try {
      const token = await getToken();
      if (!token) return;
      const response = await axios.get(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents/${id}/compliance`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const parsed = JSON.parse(response.data.complianceReport);
      setComplianceReport(parsed);
    } catch (err: any) {
      console.error("Failed to load compliance report", err);
      setComplianceError(language === "ne" ? "अनुपालन विश्लेषण लोड गर्न सकिएन।" : "Failed to load compliance report.");
    } finally {
      setComplianceLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === "compliance") {
      fetchComplianceReport();
    }
  }, [activeTab]);
  const [streamedResponse, setStreamedResponse] = useState("");

  const chatEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    async function fetchDocDetails() {
      try {
        const token = await getToken();
        if (!token) return;
        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents/${id}`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        setDoc(response.data);
      } catch (err: any) {
        console.error("Failed to load document details", err);
        setDocError(language === "ne" ? "कागजातको विवरण लोड गर्न सकिएन।" : "Failed to load document details.");
      } finally {
        setDocLoading(false);
      }
    }
    fetchDocDetails();
  }, [id, getToken, language]);

  useEffect(() => {
    async function fetchChatHistory() {
      try {
        const token = await getToken();
        if (!token) return;
        const response = await axios.get(
          `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/chat/history?docId=${id}`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        setMessages(response.data);
      } catch (err: any) {
        console.error("Failed to load chat history", err);
      } finally {
        setHistoryLoading(false);
      }
    }
    fetchChatHistory();
  }, [id, getToken]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, streamedResponse, isTyping]);

  if (docLoading) {
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

  if (docError || !doc) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-foreground px-6 text-center space-y-4">
        <div className="w-16 h-16 rounded-2xl bg-red-500/10 flex items-center justify-center">
          <ShieldAlert className="h-8 w-8 text-destructive" />
        </div>
        <h2 className="text-2xl font-serif font-bold">
          {docError || (language === "ne" ? "कागजात फेला परेन" : "Document not found")}
        </h2>
        <Link
          href="/dashboard"
          className="flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground px-6 py-2.5 font-semibold rounded-xl transition-all shadow-lg shadow-primary/25"
        >
          <ArrowLeft className="w-4 h-4" />
          {t.backToDashboard}
        </Link>
      </div>
    );
  }

  let clauses: KeyClause[] = [];
  if (doc.analysis?.keyClauses) {
    try {
      clauses = JSON.parse(doc.analysis.keyClauses);
    } catch (e) {
      console.error("Failed to parse key clauses JSON", e);
    }
  }

  const risk = doc.analysis?.riskLevel as keyof typeof riskColors | undefined;
  const rc = risk ? riskColors[risk] : riskColors.LOW;

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim() || isTyping) return;

    const userQuery = chatInput;
    setChatInput("");
    setMessages((prev) => [...prev, { role: "user", content: userQuery }]);
    setIsTyping(true);
    setStreamedResponse("");

    let fullResponse = "";

    try {
      const token = await getToken();
      if (!token) return;

      const url = `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/chat/stream?docId=${doc.id}&message=${encodeURIComponent(userQuery)}`;

      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok || !response.body) {
        throw new Error("Failed to connect to assistant stream");
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        let lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const line of lines) {
          const cleanLine = line.trim();
          if (cleanLine.startsWith("data:")) {
            const data = cleanLine.substring(5);
            if (data === "[DONE]") continue;
            fullResponse += data;
            setStreamedResponse((prev) => prev + data);
          }
        }
      }

      setMessages((prev) => [...prev, { role: "assistant", content: fullResponse }]);
      setStreamedResponse("");
    } catch (err: any) {
      console.error("Error reading chat stream", err);
      setMessages((prev) => [...prev, { role: "assistant", content: t.chatError }]);
    } finally {
      setIsTyping(false);
    }
  };

  const formatMessage = (content: string) => {
    const citationRegex = /\[Citation\s+(\d+)\]/gi;
    const parts = content.split(citationRegex);
    if (parts.length === 1) return content;

    return parts.map((part, index) => {
      if (index % 2 === 1) {
        return (
          <span
            key={index}
            className="inline-block text-[10px] bg-primary/20 border border-primary/30 text-primary px-1.5 py-0.5 rounded font-mono font-bold mx-0.5 cursor-pointer select-none"
            title={`${t.chatCitations} ${part}`}
          >
            #{part}
          </span>
        );
      }
      return part;
    });
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">

      {/* ── Header ──────────────────────────────────────────────────── */}
      <header className="sticky top-0 z-20 border-b border-border bg-background/90 backdrop-blur-xl px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link
            href="/dashboard"
            className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary transition-colors group"
          >
            <ArrowLeft className="h-4 w-4 group-hover:-translate-x-0.5 transition-transform" />
            <span className="hidden sm:block">{t.backToDashboard}</span>
          </Link>

          <span className="text-muted-foreground/30 text-lg">/</span>

          <div className="flex items-center gap-2">
            <Scale className="w-4 h-4 text-primary shrink-0" />
            <h1
              className="font-serif font-bold text-base truncate max-w-[200px] md:max-w-md"
              title={doc.fileName}
            >
              {doc.fileName}
            </h1>
          </div>

          {risk && (
            <span className={`hidden sm:inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border ${rc.bg} ${rc.text} ${rc.border}`}>
              <span className={`w-1.5 h-1.5 rounded-full ${rc.dot}`} />
              {risk}
            </span>
          )}
        </div>

        <div className="flex items-center gap-3">
          <LanguageToggler />
          <a
            href={doc.fileUrl}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-xs font-semibold bg-muted hover:bg-muted/80 border border-border px-3.5 py-2 rounded-lg transition-all"
          >
            <Download className="h-3.5 w-3.5" />
            <span className="hidden sm:block">{t.openOriginal}</span>
          </a>
        </div>
      </header>

      {/* ── Dual Panel ──────────────────────────────────────────────── */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-2 overflow-hidden" style={{ height: "calc(100vh - 57px)" }}>

        {/* ═══ LEFT PANEL ═══════════════════════════════════════════ */}
        <div className="border-r border-border flex flex-col overflow-hidden">

          {/* Tabs */}
          <div className="flex bg-card border-b border-border shrink-0">
            {[
              { key: "summary", label: `📄 ${t.docSummary}` },
              { key: "clauses", label: `⚖️ ${t.keyClauses} (${clauses.length})` },
              { key: "compliance", label: `🛡️ ${t.complianceTab}` },
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setActiveTab(key as "summary" | "clauses" | "compliance")}
                className={`flex-1 py-3.5 text-sm font-semibold border-b-2 transition-all ${
                  activeTab === key
                    ? "border-primary text-primary bg-primary/3"
                    : "border-transparent text-muted-foreground hover:text-foreground hover:bg-muted/20"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Tab body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-5">
            {activeTab === "summary" ? (
              <>
                {/* Meta cards */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-card border border-border rounded-xl p-4">
                    <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground mb-2">
                      {t.riskStatus}
                    </p>
                    {risk ? (
                      <div className="flex items-center gap-2">
                        <span className={`w-2.5 h-2.5 rounded-full ${rc.dot}`} />
                        <span className={`font-bold text-sm ${rc.text}`}>{risk}</span>
                      </div>
                    ) : (
                      <span className="text-muted-foreground text-sm">—</span>
                    )}
                  </div>
                  <div className="bg-card border border-border rounded-xl p-4">
                    <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground mb-2">
                      {t.uploadedAt}
                    </p>
                    <span className="font-semibold text-sm text-foreground">
                      {new Date(doc.createdAt).toLocaleDateString(language === "ne" ? "ne-NP" : "en-US")}
                    </span>
                  </div>
                </div>

                {/* Summary card */}
                <div className="bg-card border border-border rounded-xl p-6">
                  <div className="flex items-center gap-2 mb-4">
                    <FileText className="w-4 h-4 text-primary" />
                    <h2 className="font-serif font-bold text-sm text-foreground">{t.docSummary}</h2>
                  </div>
                  {doc.analysis?.summary ? (
                    <div className="space-y-3">
                      {doc.analysis.summary.split("\n").map((para, index) =>
                        para.trim() ? (
                          <p key={index} className="text-sm text-foreground/85 leading-relaxed font-light">
                            {para}
                          </p>
                        ) : null
                      )}
                    </div>
                  ) : (
                    <p className="text-muted-foreground italic text-sm">{t.noSummary}</p>
                  )}
                </div>
              </>
            ) : activeTab === "clauses" ? (
              <div className="space-y-4">
                {clauses.length === 0 ? (
                  <div className="text-center py-16 rounded-xl border border-dashed border-border text-muted-foreground">
                    <Scale className="w-10 h-10 mx-auto mb-3 opacity-25" />
                    <p className="text-sm">{t.noClauses}</p>
                  </div>
                ) : (
                  clauses.map((clause, index) => {
                    const cc = clauseRiskColors[clause.risk];
                    return (
                      <div
                        key={index}
                        className="bg-card border border-border rounded-xl overflow-hidden hover:border-border/60 hover:shadow-md transition-all"
                      >
                        {/* Colored top accent bar */}
                        <div className={`h-1 w-full ${cc.bar}`} />

                        <div className="p-5 space-y-3">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-mono font-bold text-muted-foreground/60">
                              #{index + 1}
                            </span>
                            <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full border ${cc.bg} ${cc.text} ${cc.border}`}>
                              {clause.risk === "DANGER" && <ShieldAlert className="h-3 w-3" />}
                              {clause.risk === "REVIEW" && <Shield className="h-3 w-3" />}
                              {clause.risk === "SAFE" && <ShieldCheck className="h-3 w-3" />}
                              {clause.risk}
                            </span>
                          </div>

                          <blockquote className="relative pl-3 border-l-2 border-primary/50 text-sm italic text-foreground/80 leading-relaxed">
                            "{clause.text}"
                          </blockquote>

                          <p className="text-sm text-foreground/75 leading-relaxed font-light">
                            {clause.explanation}
                          </p>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            ) : (
              <div className="space-y-4 animate-fade-in">
                <div className="bg-card border border-border rounded-xl p-5 space-y-2">
                  <h3 className="font-serif font-bold text-sm text-foreground">{t.complianceTitle}</h3>
                  <p className="text-xs text-muted-foreground font-light leading-relaxed">
                    {language === "ne"
                      ? "यो रिपोर्टले नेपाल श्रम ऐन २०७४ का प्रमुख प्रावधानहरूसँग यस सम्झौताका बुँदाहरूको तुलना गर्दछ।"
                      : "This report evaluates agreement clauses against key statutory minimums of the Nepal Labor Act 2074."}
                  </p>
                </div>
                {complianceLoading ? (
                  <div className="py-12 flex flex-col items-center justify-center gap-3">
                    <Loader2 className="h-6 w-6 text-primary animate-spin" />
                    <span className="text-xs text-muted-foreground">{t.loadingCompliance}</span>
                  </div>
                ) : complianceError ? (
                  <div className="py-8 text-center bg-red-500/10 border border-red-500/20 text-destructive rounded-xl text-xs font-semibold px-4">
                    {complianceError}
                  </div>
                ) : (
                  complianceReport.map((item, index) => {
                    const statusText = {
                      COMPLIANT: t.compliant,
                      NON_COMPLIANT: t.nonCompliant,
                      NOT_SPECIFIED: t.notSpecified,
                    }[item.status as "COMPLIANT" | "NON_COMPLIANT" | "NOT_SPECIFIED"] || item.status;

                    const cardColors = {
                      COMPLIANT: "border-green-500/20 hover:border-green-500/40 bg-green-500/1 hover:shadow-green-500/2",
                      NON_COMPLIANT: "border-red-500/20 hover:border-red-500/40 bg-red-500/1 hover:shadow-red-500/2",
                      NOT_SPECIFIED: "border-yellow-500/20 hover:border-yellow-500/40 bg-yellow-500/1 hover:shadow-yellow-500/2",
                    }[item.status as "COMPLIANT" | "NON_COMPLIANT" | "NOT_SPECIFIED"] || "border-border";

                    return (
                      <div
                        key={index}
                        className={`bg-card border rounded-xl p-5 space-y-3 transition-all ${cardColors}`}
                      >
                        <div className="flex items-center justify-between">
                          <h4 className="font-serif font-bold text-sm text-foreground">{item.provision}</h4>
                          <span className="text-xs font-semibold">{statusText}</span>
                        </div>

                        {item.status !== "NOT_SPECIFIED" && (
                          <blockquote className="pl-3 border-l-2 border-primary/50 text-xs italic text-foreground/80 leading-relaxed">
                            "{item.clauseText}"
                          </blockquote>
                        )}

                        <div className="text-xs text-foreground/75 leading-relaxed font-light font-sans bg-muted/20 p-3 rounded-lg border border-border/40">
                          <span className="font-semibold block mb-1">{t.explanation}:</span>
                          {item.explanation}
                          <span className="block mt-2 font-mono text-[10px] text-muted-foreground">
                            {t.lawReference}: {item.lawReference}
                          </span>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            )}
          </div>
        </div>

        {/* ═══ RIGHT PANEL — AI CHAT ════════════════════════════════ */}
        <div className="flex flex-col bg-muted/5 overflow-hidden">

          {/* Chat header */}
          <div className="border-b border-border bg-card px-6 py-4 flex items-center gap-3 shrink-0">
            <div className="w-9 h-9 rounded-xl bg-primary/15 border border-primary/25 flex items-center justify-center">
              <Bot className="w-4.5 h-4.5 text-primary" />
            </div>
            <div>
              <h2 className="font-serif font-bold text-sm text-foreground leading-none">
                {t.chatbotTitle}
              </h2>
              <p className="text-[11px] text-muted-foreground mt-0.5">{t.chatbotSub}</p>
            </div>
            <div className="ml-auto flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <span className="text-[11px] text-muted-foreground font-medium">Online</span>
            </div>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-5 space-y-4">
            {historyLoading ? (
              <div className="h-full flex items-center justify-center">
                <Loader2 className="h-6 w-6 text-primary animate-spin" />
              </div>
            ) : messages.length === 0 && !streamedResponse ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-4">
                <div className="w-16 h-16 rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center">
                  <Sparkles className="w-7 h-7 text-primary" />
                </div>
                <div className="space-y-1.5">
                  <h3 className="font-bold text-sm text-foreground">
                    {language === "ne" ? "एआई सहायकसँग सोध्नुहोस्" : "Ask the AI Assistant"}
                  </h3>
                  <p className="text-xs text-muted-foreground max-w-xs leading-relaxed">
                    {language === "ne"
                      ? "यस कागजातको बारेमा कुनै पनि प्रश्न सोध्नुहोस्।"
                      : "Ask any questions about this document and the AI will analyze it to respond."}
                  </p>
                </div>
                {/* Suggestion chips */}
                <div className="flex flex-wrap gap-2 justify-center mt-2">
                  {["Summarize key risks", "Explain clause 1", "Is this contract fair?"].map((suggestion) => (
                    <button
                      key={suggestion}
                      onClick={() => setChatInput(suggestion)}
                      className="text-xs px-3 py-1.5 rounded-full border border-border hover:border-primary/50 hover:bg-primary/5 text-muted-foreground hover:text-primary transition-all"
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {messages.map((msg, i) => (
                  <div
                    key={i}
                    className={`flex items-end gap-2.5 ${msg.role === "user" ? "justify-end" : "justify-start"}`}
                  >
                    {msg.role === "assistant" && (
                      <div className="w-7 h-7 rounded-lg bg-primary/15 border border-primary/20 flex items-center justify-center shrink-0 mb-0.5">
                        <Bot className="w-3.5 h-3.5 text-primary" />
                      </div>
                    )}

                    <div
                      className={`max-w-[82%] px-4 py-3 rounded-2xl text-sm leading-relaxed shadow-sm ${
                        msg.role === "user"
                          ? "bg-primary text-primary-foreground rounded-br-sm"
                          : "bg-card border border-border text-foreground/90 font-light rounded-bl-sm"
                      }`}
                    >
                      <p className="whitespace-pre-wrap">{formatMessage(msg.content)}</p>
                    </div>

                    {msg.role === "user" && (
                      <div className="w-7 h-7 rounded-lg bg-muted border border-border flex items-center justify-center shrink-0 mb-0.5">
                        <User className="w-3.5 h-3.5 text-muted-foreground" />
                      </div>
                    )}
                  </div>
                ))}

                {/* Streamed response */}
                {streamedResponse && (
                  <div className="flex items-end gap-2.5 justify-start">
                    <div className="w-7 h-7 rounded-lg bg-primary/15 border border-primary/20 flex items-center justify-center shrink-0">
                      <Bot className="w-3.5 h-3.5 text-primary" />
                    </div>
                    <div className="max-w-[82%] px-4 py-3 rounded-2xl rounded-bl-sm bg-card border border-border text-sm text-foreground/90 font-light shadow-sm">
                      <p className="whitespace-pre-wrap">{formatMessage(streamedResponse)}</p>
                      <span className="inline-block w-1.5 h-4 bg-primary/60 animate-pulse ml-0.5 rounded-sm" />
                    </div>
                  </div>
                )}

                {/* Typing indicator */}
                {isTyping && !streamedResponse && (
                  <div className="flex items-end gap-2.5 justify-start">
                    <div className="w-7 h-7 rounded-lg bg-primary/15 border border-primary/20 flex items-center justify-center shrink-0">
                      <Bot className="w-3.5 h-3.5 text-primary" />
                    </div>
                    <div className="bg-card border border-border px-4 py-3.5 rounded-2xl rounded-bl-sm flex items-center gap-1.5 shadow-sm">
                      <span className="w-2 h-2 rounded-full bg-primary/60 typing-dot" />
                      <span className="w-2 h-2 rounded-full bg-primary/60 typing-dot" />
                      <span className="w-2 h-2 rounded-full bg-primary/60 typing-dot" />
                    </div>
                  </div>
                )}

                <div ref={chatEndRef} />
              </div>
            )}
          </div>

          {/* Input bar */}
          <form
            onSubmit={handleSendMessage}
            className="border-t border-border p-4 bg-card flex gap-3 shrink-0"
          >
            <div className="flex-1 relative">
              <input
                type="text"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                placeholder={t.chatPlaceholder}
                disabled={isTyping}
                className="w-full border border-border bg-background rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary transition-all disabled:bg-muted/40 disabled:cursor-not-allowed placeholder:text-muted-foreground/60"
              />
            </div>
            <button
              type="submit"
              disabled={!chatInput.trim() || isTyping}
              className="w-11 h-11 shrink-0 flex items-center justify-center bg-primary hover:bg-primary/90 text-primary-foreground rounded-xl transition-all shadow-md shadow-primary/25 hover:shadow-primary/40 disabled:opacity-40 disabled:cursor-not-allowed active:scale-95"
            >
              {isTyping ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
            </button>
          </form>
        </div>

      </div>
    </div>
  );
}
