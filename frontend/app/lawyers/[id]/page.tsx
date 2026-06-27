"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth, useUser, UserButton } from "@clerk/nextjs";
import axios from "axios";
import {
  Users, MapPin, Award, Star, Clock, ChevronRight, ArrowLeft,
  LayoutDashboard, FileStack, Scale, ShieldCheck, FileText, AlertTriangle, Sparkles
} from "lucide-react";
import Link from "next/link";
import { useSelector } from "react-redux";
import { RootState } from "../../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

interface Lawyer {
  id: string;
  name: string;
  email: string;
  phone: string;
  specialization: "CIVIL" | "LABOUR" | "CORPORATE";
  rating: number;
  hourlyRate: number;
  bio: string;
  location: string;
  experienceYears: number;
  availability: string; // JSON array of slots
  isVerified: boolean;
}

interface Document {
  id: string;
  fileName: string;
  status: string;
}

interface BookingResponse {
  id: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  status: string;
  paymentStatus: string;
  complexityRating: "LOW" | "MEDIUM" | "HIGH";
  complexityReport: string;
}

const navItems = [
  { href: "/dashboard", icon: LayoutDashboard, labelKey: "dashboard" },
  { href: "/templates", icon: FileStack, labelKey: "templates" },
  { href: "/lawyers", icon: Users, labelKey: "findLawyersSidebar" },
];

export default function LawyerProfile() {
  const params = useParams();
  const router = useRouter();
  const lawyerId = params.id as string;

  const { getToken } = useAuth();
  const { user } = useUser();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [lawyer, setLawyer] = useState<Lawyer | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);

  // Form states
  const [selectedSlot, setSelectedSlot] = useState<string>("");
  const [explanation, setExplanation] = useState<string>("");
  const [selectedDocId, setSelectedDocId] = useState<string>("");
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingError, setBookingError] = useState<string | null>(null);

  // AI pre-screening result state
  const [screeningResult, setScreeningResult] = useState<BookingResponse | null>(null);

  const fetchDetails = useCallback(async () => {
    try {
      setLoading(true);
      const token = await getToken();
      if (!token) return;

      // 1. Fetch Lawyer
      const lawyerRes = await axios.get(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/lawyers/${lawyerId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setLawyer(lawyerRes.data);

      // 2. Fetch User Documents
      const docsRes = await axios.get(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/documents`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      // Filter done documents
      setDocuments(docsRes.data.filter((d: Document) => d.status === "DONE"));

    } catch (err) {
      console.error("Failed to load lawyer profile / documents", err);
    } finally {
      setLoading(false);
    }
  }, [getToken, lawyerId]);

  useEffect(() => {
    if (lawyerId) {
      fetchDetails();
    }
  }, [lawyerId, fetchDetails]);

  // Parse availability slots
  let slots: string[] = [];
  if (lawyer?.availability) {
    try {
      slots = JSON.parse(lawyer.availability);
    } catch (e) {
      console.error("Error parsing slots json", e);
    }
  }

  const handleBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSlot) {
      setBookingError("Please select a time slot.");
      return;
    }
    if (!explanation.trim()) {
      setBookingError("Please explain your legal issue.");
      return;
    }

    setBookingLoading(true);
    setBookingError(null);

    try {
      const token = await getToken();
      if (!token) throw new Error("Authentication error");

      // Split slot into date and time (e.g. "2026-06-28 10:00")
      const [datePart, timePart] = selectedSlot.split(" ");
      const [hour, min] = timePart.split(":");
      const endHour = String(Number(hour) + 1).padStart(2, "0");
      const endTimePart = `${endHour}:${min}`;

      const payload = {
        lawyerId: lawyer?.id,
        bookingDate: datePart,
        startTime: timePart,
        endTime: endTimePart,
        userExplanation: explanation,
        associatedDocId: selectedDocId || null
      };

      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/bookings`,
        payload,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setScreeningResult(response.data);
    } catch (err: any) {
      console.error("Booking failed", err);
      setBookingError(err.response?.data?.message || "Booking request failed. Please check inputs.");
    } finally {
      setBookingLoading(false);
    }
  };

  const handleProceedToPayment = () => {
    if (!screeningResult || !lawyer) return;
    router.push(`/billing?bookingId=${screeningResult.id}&amount=${lawyer.hourlyRate}`);
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background text-foreground">
        <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  if (!lawyer) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-background text-foreground gap-4">
        <p className="text-muted-foreground">Lawyer profile not found.</p>
        <Link href="/lawyers" className="text-primary hover:underline flex items-center gap-1">
          <ArrowLeft className="w-4 h-4" /> Back to directory
        </Link>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* ── Sidebar ─────────────────────────────────────────────────── */}
      <aside className="w-64 shrink-0 border-r border-border bg-card hidden md:flex flex-col justify-between">
        <div className="p-6 space-y-8">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center shadow-lg">
              <Scale className="w-4 h-4 text-primary-foreground" />
            </div>
            <span className="text-lg font-serif font-bold text-foreground">LegalEase</span>
          </div>

          <nav className="space-y-1">
            {navItems.map(({ href, icon: Icon, labelKey }) => {
              const isActive = href === "/lawyers";
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
        </div>

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
        {/* Top bar */}
        <header className="border-b border-border px-6 py-4 flex items-center justify-between md:px-8 bg-background/80 backdrop-blur sticky top-0 z-10">
          <div className="flex items-center gap-3">
            <Link href="/lawyers" className="text-muted-foreground hover:text-foreground mr-1">
              <ArrowLeft className="w-5 h-5" />
            </Link>
            <h1 className="text-xl font-serif font-bold text-foreground">
              {lawyer.name}
            </h1>
          </div>
          <div className="flex items-center gap-3 md:hidden">
            <LanguageToggler />
            <UserButton afterSignOutUrl="/" />
          </div>
        </header>

        <div className="p-6 md:p-8 max-w-4xl mx-auto space-y-8">
          {/* Lawyer Header Bio */}
          <div className="bg-card border border-border rounded-2xl p-6 relative overflow-hidden">
            <div className="absolute inset-0 opacity-10 pointer-events-none"
              style={{
                background: "radial-gradient(ellipse at top right, oklch(0.555 0.163 49 / 0.3) 0%, transparent 70%)"
              }}
            />
            <div className="relative flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary text-2xl">
                  {lawyer.name.split(" ").map(n => n[0]).join("")}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-2xl font-serif font-bold text-foreground">{lawyer.name}</h2>
                    {lawyer.isVerified && (
                      <span className="text-[10px] text-green-500 font-bold bg-green-500/10 border border-green-500/20 px-2 py-0.5 rounded-full flex items-center gap-0.5">
                        <ShieldCheck className="w-3 h-3" />
                        {t.verifiedLawyer}
                      </span>
                    )}
                  </div>
                  <p className="text-sm font-semibold text-primary uppercase tracking-wider mt-1">
                    {lawyer.specialization} Law Specialist
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-4 text-xs font-semibold">
                <div className="bg-muted px-4 py-2.5 rounded-xl border border-border text-center">
                  <p className="text-muted-foreground text-[10px] uppercase font-bold tracking-widest mb-0.5">Experience</p>
                  <p className="text-foreground text-sm font-bold">{lawyer.experienceYears} Years</p>
                </div>
                <div className="bg-muted px-4 py-2.5 rounded-xl border border-border text-center">
                  <p className="text-muted-foreground text-[10px] uppercase font-bold tracking-widest mb-0.5">Consultation</p>
                  <p className="text-foreground text-sm font-bold">Rs. {lawyer.hourlyRate.toLocaleString()} {t.hourlyRateVal}</p>
                </div>
              </div>
            </div>

            <p className="text-sm text-muted-foreground leading-relaxed mt-6 border-t border-border/50 pt-4">
              {lawyer.bio}
            </p>
          </div>

          {/* Booking & AI Pre-screening Panel */}
          {!screeningResult ? (
            <div className="bg-card border border-border rounded-2xl p-6 md:p-8 space-y-6">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                  <Clock className="w-4 h-4 text-primary" />
                </div>
                <h3 className="text-lg font-serif font-bold text-foreground">{t.bookAppointmentTitle}</h3>
              </div>

              <form onSubmit={handleBooking} className="space-y-6">
                {/* 1. Time Slots Select */}
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-foreground">{t.selectDateTime}</label>
                  {slots.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No time slots currently available for this lawyer.</p>
                  ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                      {slots.map((slot) => {
                        const isSelected = selectedSlot === slot;
                        return (
                          <button
                            key={slot}
                            type="button"
                            onClick={() => setSelectedSlot(slot)}
                            className={`py-2 px-3 text-xs font-semibold rounded-xl border transition-all ${
                              isSelected
                                ? "bg-primary text-primary-foreground border-primary shadow-md shadow-primary/20"
                                : "bg-background border-border hover:border-primary/50 text-muted-foreground hover:text-foreground"
                            }`}
                          >
                            {slot}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* 2. Case Explanation */}
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-foreground flex items-center gap-1.5">
                    {t.caseExplanationLabel}
                    <Sparkles className="w-4 h-4 text-primary animate-pulse" />
                  </label>
                  <textarea
                    rows={4}
                    placeholder={t.caseExplanationPlaceholder}
                    value={explanation}
                    onChange={(e) => setExplanation(e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all resize-none leading-relaxed"
                  />
                </div>

                {/* 3. Document Attachment */}
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-foreground">{t.associatedDocLabel}</label>
                  <div className="relative">
                    <select
                      value={selectedDocId}
                      onChange={(e) => setSelectedDocId(e.target.value)}
                      className="w-full px-4 py-2.5 bg-background border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all appearance-none cursor-pointer"
                    >
                      <option value="">{t.associatedDocNone}</option>
                      {documents.map((doc) => (
                        <option key={doc.id} value={doc.id}>
                          {doc.fileName}
                        </option>
                      ))}
                    </select>
                    <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none border-l border-border pl-2">
                      <FileText className="w-4 h-4 text-muted-foreground" />
                    </div>
                  </div>
                </div>

                {bookingError && (
                  <div className="p-3 text-red-500 bg-red-500/10 border border-red-500/20 rounded-xl text-xs font-semibold">
                    {bookingError}
                  </div>
                )}

                {/* Submit button */}
                <button
                  type="submit"
                  disabled={bookingLoading}
                  className="w-full py-3 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all flex items-center justify-center gap-2 text-sm disabled:opacity-50"
                >
                  {bookingLoading ? (
                    <>
                      <div className="w-4 h-4 border-2 border-primary-foreground border-t-transparent rounded-full animate-spin" />
                      Creating consultation booking & running AI Pre-screening...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      {t.confirmBookingAndPreScreen}
                    </>
                  )}
                </button>
              </form>
            </div>
          ) : (
            /* ── AI Pre-screening & Report Results Screen ── */
            <div className="space-y-6">
              {/* Success Notification Alert */}
              <div className="bg-green-500/10 border border-green-500/20 text-green-500 rounded-2xl p-4 flex items-center gap-3 text-sm font-semibold">
                <ShieldCheck className="w-5 h-5 shrink-0" />
                <span>{t.bookingSuccessMsg}</span>
              </div>

              {/* Pre-screening Report Container */}
              <div className="bg-card border border-border rounded-2xl p-6 md:p-8 space-y-6 relative overflow-hidden">
                {/* Visual indicator corner */}
                <div className="absolute top-0 right-0 w-24 h-24 pointer-events-none"
                  style={{
                    background: "radial-gradient(circle at 100% 0%, oklch(0.555 0.163 49 / 0.1) 0%, transparent 70%)"
                  }}
                />

                {/* Report Header */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
                  <div className="flex items-center gap-2">
                    <Sparkles className="w-5 h-5 text-primary animate-pulse" />
                    <h3 className="text-lg font-serif font-bold text-foreground">{t.complexityReportLabel}</h3>
                  </div>
                  
                  {/* Exposure Risk Level */}
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground">{t.complexityRatingLabel}:</span>
                    <span className={`inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1 rounded-full border ${
                      screeningResult.complexityRating === "HIGH"
                        ? "bg-red-500/10 text-red-500 border-red-500/20"
                        : screeningResult.complexityRating === "MEDIUM"
                        ? "bg-yellow-500/10 text-yellow-500 border-yellow-500/20"
                        : "bg-green-500/10 text-green-500 border-green-500/20"
                    }`}>
                      <AlertTriangle className="w-3.5 h-3.5" />
                      {screeningResult.complexityRating} RISK
                    </span>
                  </div>
                </div>

                {/* Report Content */}
                <div className="prose prose-invert max-w-none text-sm text-foreground/80 leading-relaxed space-y-4 whitespace-pre-line bg-muted/30 p-5 rounded-xl border border-border">
                  {screeningResult.complexityReport}
                </div>

                {/* Payment button */}
                <button
                  onClick={handleProceedToPayment}
                  className="w-full py-3 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all flex items-center justify-center gap-2 text-sm"
                >
                  Proceed to Payment
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
