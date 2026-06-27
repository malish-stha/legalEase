"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useAuth, useUser, UserButton } from "@clerk/nextjs";
import axios from "axios";
import {
  CreditCard, Wallet, ShieldCheck, ArrowLeft, Loader2, Scale, LayoutDashboard, FileStack, Users, ChevronRight
} from "lucide-react";
import Link from "next/link";
import { useSelector } from "react-redux";
import { RootState } from "../../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";

const navItems = [
  { href: "/dashboard", icon: LayoutDashboard, labelKey: "dashboard" },
  { href: "/templates", icon: FileStack, labelKey: "templates" },
  { href: "/lawyers", icon: Users, labelKey: "findLawyersSidebar" },
];

function BillingCheckoutContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const bookingId = searchParams.get("bookingId");
  const amountParam = searchParams.get("amount");
  const amount = amountParam ? parseFloat(amountParam) : 500.00; // Default amount if not specified

  const { getToken } = useAuth();
  const { user } = useUser();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleEsewaCheckout = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = await getToken();
      if (!token) throw new Error("Authentication failed");

      // Initiate eSewa payment from backend
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/billing/esewa/initiate`,
        null,
        {
          headers: { Authorization: `Bearer ${token}` },
          params: { bookingId, amount }
        }
      );

      const params = response.data;

      // Create a hidden form in DOM and submit it to redirect to eSewa sandbox
      const form = document.createElement("form");
      form.setAttribute("method", "POST");
      form.setAttribute("action", "https://rc-epay.esewa.com.np/api/epay/main/v2/form");

      Object.keys(params).forEach((key) => {
        const input = document.createElement("input");
        input.setAttribute("type", "hidden");
        input.setAttribute("name", key);
        input.setAttribute("value", params[key]);
        form.appendChild(input);
      });

      document.body.appendChild(form);
      form.submit();
    } catch (err: any) {
      console.error("eSewa initiation failed", err);
      setError("Failed to initiate eSewa payment session. Please try again.");
      setLoading(false);
    }
  };

  const handleKhaltiCheckout = async () => {
    setLoading(true);
    setError(null);
    try {
      const token = await getToken();
      if (!token) throw new Error("Authentication failed");

      // Initiate Khalti payment from backend
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/billing/khalti/initiate`,
        null,
        {
          headers: { Authorization: `Bearer ${token}` },
          params: { bookingId, amount }
        }
      );

      const { payment_url } = response.data;
      if (payment_url) {
        // Redirect to Khalti's checkout web page
        window.location.href = payment_url;
      } else {
        throw new Error("No checkout URL returned from Khalti");
      }
    } catch (err: any) {
      console.error("Khalti initiation failed", err);
      setError("Failed to initiate Khalti payment session. Please try again.");
      setLoading(false);
    }
  };

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
              const isActive = href === "/lawyers"; // Group billing under lawyers marketplace flow
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
            <button onClick={() => router.back()} className="text-muted-foreground hover:text-foreground mr-1">
              <ArrowLeft className="w-5 h-5" />
            </button>
            <h1 className="text-xl font-serif font-bold text-foreground">{t.billingTitle}</h1>
          </div>
          <div className="flex items-center gap-3 md:hidden">
            <LanguageToggler />
            <UserButton afterSignOutUrl="/" />
          </div>
        </header>

        <div className="p-6 md:p-8 max-w-2xl mx-auto space-y-8">
          <div className="space-y-2">
            <p className="text-muted-foreground">
              {t.billingSub}
            </p>
          </div>

          {/* ── Order Summary Card ── */}
          <div className="bg-card border border-border rounded-2xl p-6 shadow-sm space-y-4">
            <h3 className="font-bold text-foreground text-sm uppercase tracking-wider text-muted-foreground">Checkout Summary</h3>
            <div className="border-t border-border/60 pt-4 space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Item description</span>
                <span className="font-semibold text-foreground">
                  {bookingId ? "Lawyer Consultation Booking" : "Legal Document Analysis Plan"}
                </span>
              </div>
              {bookingId && (
                <div className="flex justify-between text-xs">
                  <span className="text-muted-foreground">Reference ID</span>
                  <span className="font-mono text-muted-foreground/80 truncate max-w-[200px]">{bookingId}</span>
                </div>
              )}
              <div className="border-t border-border/40 pt-3 flex justify-between items-baseline">
                <span className="text-sm font-semibold text-foreground">Total Amount</span>
                <span className="text-2xl font-bold text-primary">Rs. {amount.toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* ── Payment Options ── */}
          <div className="space-y-4">
            {error && (
              <div className="p-4 text-sm font-semibold text-red-500 bg-red-500/10 border border-red-500/20 rounded-2xl">
                {error}
              </div>
            )}

            {loading ? (
              <div className="bg-card border border-border rounded-2xl p-10 flex flex-col items-center justify-center gap-4">
                <Loader2 className="w-8 h-8 text-primary animate-spin" />
                <p className="text-sm text-muted-foreground">Initiating secure transaction session...</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* eSewa checkout option */}
                <button
                  onClick={handleEsewaCheckout}
                  className="group relative bg-green-600 hover:bg-green-600/90 text-white rounded-2xl p-6 flex flex-col items-center justify-center gap-3 transition-all shadow-lg hover:shadow-green-600/10 border border-green-500/20"
                >
                  <div className="w-12 h-12 rounded-full bg-white flex items-center justify-center shadow-md">
                    <span className="text-green-600 font-extrabold text-lg tracking-tighter">e</span>
                  </div>
                  <div className="text-center">
                    <p className="font-bold text-sm tracking-wide">eSewa Wallet</p>
                    <p className="text-[10px] opacity-80 mt-0.5">{t.payWithEsewa}</p>
                  </div>
                </button>

                {/* Khalti checkout option */}
                <button
                  onClick={handleKhaltiCheckout}
                  className="group relative bg-indigo-700 hover:bg-indigo-700/90 text-white rounded-2xl p-6 flex flex-col items-center justify-center gap-3 transition-all shadow-lg hover:shadow-indigo-700/10 border border-indigo-500/20"
                >
                  <div className="w-12 h-12 rounded-full bg-white flex items-center justify-center shadow-md">
                    <span className="text-indigo-700 font-extrabold text-lg tracking-tighter">K</span>
                  </div>
                  <div className="text-center">
                    <p className="font-bold text-sm tracking-wide">Khalti Wallet</p>
                    <p className="text-[10px] opacity-80 mt-0.5">{t.payWithKhalti}</p>
                  </div>
                </button>
              </div>
            )}
          </div>

          <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
            <ShieldCheck className="w-4 h-4 text-green-500" />
            <span>Secure 256-bit SSL encrypted local transaction pipeline</span>
          </div>
        </div>
      </main>
    </div>
  );
}

export default function BillingCheckout() {
  return (
    <Suspense fallback={
      <div className="flex h-screen items-center justify-center bg-background text-foreground">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    }>
      <BillingCheckoutContent />
    </Suspense>
  );
}
