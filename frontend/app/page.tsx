"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { SignedIn, SignedOut, UserButton } from "@clerk/nextjs";
import { useSelector } from "react-redux";
import { RootState } from "../store/store";
import { translations } from "@/lib/translations";
import LanguageToggler from "@/components/LanguageToggler";
import { ArrowRight, FileText, MessageSquare, Users, Shield, Zap, Scale } from "lucide-react";

const stats = [
  { value: "10K+", label: "Documents Analyzed" },
  { value: "98%", label: "Accuracy Rate" },
  { value: "2 min", label: "Avg. Analysis Time" },
  { value: "500+", label: "Lawyers Connected" },
];

export default function Home() {
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <div className="flex flex-col min-h-screen bg-background text-foreground overflow-x-hidden">

      {/* ── Navigation ─────────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center shadow-lg group-hover:shadow-primary/40 transition-shadow">
              <Scale className="w-4 h-4 text-primary-foreground" />
            </div>
            <span className="text-xl font-serif font-bold text-foreground tracking-tight">LegalEase</span>
            <span className="hidden sm:block text-[10px] font-medium px-1.5 py-0.5 rounded bg-primary/10 text-primary border border-primary/20">
              Nepal
            </span>
          </Link>

          <nav className="flex items-center gap-5">
            <LanguageToggler />

            <SignedOut>
              <Link href="/sign-in" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                {t.login}
              </Link>
              <Link href="/sign-up" className="relative text-sm font-semibold px-5 py-2.5 rounded-lg bg-primary text-primary-foreground shadow-lg shadow-primary/25 hover:shadow-primary/40 hover:bg-primary/90 transition-all duration-200 active:scale-95">
                {t.register}
              </Link>
            </SignedOut>

            <SignedIn>
              <Link
                href="/dashboard"
                className="text-sm font-semibold px-5 py-2.5 rounded-lg bg-primary text-primary-foreground shadow-lg shadow-primary/25 hover:shadow-primary/40 hover:bg-primary/90 transition-all duration-200"
              >
                {t.dashboard}
              </Link>
              <UserButton afterSignOutUrl="/" />
            </SignedIn>
          </nav>
        </div>
      </header>

      {/* ── Hero Section ────────────────────────────────────────────── */}
      <section className="relative flex-1 overflow-hidden">

        {/* Ambient gradient orbs */}
        <div className="pointer-events-none absolute inset-0 overflow-hidden">
          <div
            className="absolute -top-32 -left-32 w-[600px] h-[600px] rounded-full opacity-30 animate-orb-pulse"
            style={{ background: "radial-gradient(circle, oklch(0.65 0.18 50) 0%, transparent 70%)" }}
          />
          <div
            className="absolute top-1/2 -right-48 w-[500px] h-[500px] rounded-full opacity-20 animate-orb-pulse delay-700"
            style={{ background: "radial-gradient(circle, oklch(0.55 0.16 45) 0%, transparent 70%)" }}
          />
          <div
            className="absolute -bottom-24 left-1/3 w-[400px] h-[400px] rounded-full opacity-15 animate-orb-pulse delay-400"
            style={{ background: "radial-gradient(circle, oklch(0.70 0.12 80) 0%, transparent 70%)" }}
          />
        </div>

        {/* Grid pattern overlay */}
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.03]"
          style={{
            backgroundImage:
              "linear-gradient(hsl(0,0%,0%) 1px, transparent 1px), linear-gradient(90deg, hsl(0,0%,0%) 1px, transparent 1px)",
            backgroundSize: "64px 64px",
          }}
        />

        <div className="relative max-w-7xl mx-auto px-6 pt-20 pb-32">
          {/* Badge pill */}
          <div
            className={`inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-primary/30 bg-primary/8 text-primary text-sm font-medium mb-8 ${
              mounted ? "animate-badge-pop" : "opacity-0"
            }`}
          >
            <Zap className="w-3.5 h-3.5 fill-current" />
            AI-Powered Legal Document Analysis
          </div>

          {/* Headline */}
          <div
            className={`max-w-4xl ${mounted ? "animate-slide-up" : "opacity-0"}`}
          >
            <h1 className="text-5xl sm:text-6xl md:text-7xl font-serif font-bold tracking-tight text-foreground leading-[1.08] mb-6">
              {language === "ne" ? (
                <>
                  कानूनी दस्तावेजहरूलाई{" "}
                  <span
                    className="animate-shimmer"
                    style={{
                      background:
                        "linear-gradient(90deg, oklch(0.555 0.163 49) 0%, oklch(0.75 0.14 70) 40%, oklch(0.555 0.163 49) 80%)",
                      backgroundSize: "200% auto",
                      WebkitBackgroundClip: "text",
                      WebkitTextFillColor: "transparent",
                      backgroundClip: "text",
                    }}
                  >
                    सरल भाषामा
                  </span>{" "}
                  बुझ्नुहोस्
                </>
              ) : (
                <>
                  Understand Legal{" "}
                  <span
                    className="animate-shimmer"
                    style={{
                      background:
                        "linear-gradient(90deg, oklch(0.555 0.163 49) 0%, oklch(0.75 0.14 70) 40%, oklch(0.555 0.163 49) 80%)",
                      backgroundSize: "200% auto",
                      WebkitBackgroundClip: "text",
                      WebkitTextFillColor: "transparent",
                      backgroundClip: "text",
                    }}
                  >
                    Documents
                  </span>{" "}
                  in Simple Language
                </>
              )}
            </h1>

            <p className="text-xl text-muted-foreground font-light max-w-2xl leading-relaxed mb-10">
              {t.subtitle}
            </p>
          </div>

          {/* CTA Buttons */}
          <div
            className={`flex flex-col sm:flex-row gap-4 items-start ${
              mounted ? "animate-slide-up delay-200" : "opacity-0"
            }`}
          >
            <SignedOut>
              <Link href="/sign-up" className="group flex items-center gap-2 text-base font-semibold px-8 py-4 rounded-xl bg-primary text-primary-foreground shadow-xl shadow-primary/30 hover:shadow-primary/50 hover:bg-primary/90 transition-all duration-300 active:scale-95">
                {t.getStarted}
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </Link>
              <Link href="/sign-in" className="flex items-center gap-2 text-base font-medium px-8 py-4 rounded-xl border border-border hover:bg-muted/50 transition-all duration-200">
                {t.login}
              </Link>
            </SignedOut>

            <SignedIn>
              <Link href="/dashboard">
                <button className="group flex items-center gap-2 text-base font-semibold px-8 py-4 rounded-xl bg-primary text-primary-foreground shadow-xl shadow-primary/30 hover:shadow-primary/50 hover:bg-primary/90 transition-all duration-300">
                  {t.goToDashboard}
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </button>
              </Link>
            </SignedIn>

            <Link href="/lawyers">
              <button className="flex items-center gap-2 text-base font-medium px-8 py-4 rounded-xl border border-border hover:bg-muted/50 transition-all duration-200">
                {t.findLawyers}
              </button>
            </Link>
          </div>

          {/* Floating document preview cards */}
          <div className="mt-20 relative hidden md:block">
            {/* Main card */}
            <div
              className={`relative max-w-2xl mx-auto ${
                mounted ? "animate-float" : ""
              }`}
            >
              <div className="bg-card border border-border rounded-2xl shadow-2xl p-6 overflow-hidden">
                {/* Faux document header */}
                <div className="flex items-center gap-3 mb-5 pb-4 border-b border-border">
                  <div className="w-9 h-9 rounded-lg bg-primary/15 flex items-center justify-center">
                    <FileText className="w-4.5 h-4.5 text-primary" />
                  </div>
                  <div>
                    <div className="h-3 w-48 bg-muted rounded-full mb-1.5" />
                    <div className="h-2.5 w-32 bg-muted/60 rounded-full" />
                  </div>
                  <span className="ml-auto text-xs font-semibold px-2.5 py-1 rounded-full bg-green-500/10 text-green-500 border border-green-500/20">
                    ✓ Analyzed
                  </span>
                </div>
                {/* Faux content lines */}
                <div className="space-y-2.5 mb-5">
                  <div className="h-2.5 bg-muted rounded-full w-full" />
                  <div className="h-2.5 bg-muted rounded-full w-5/6" />
                  <div className="h-2.5 bg-muted rounded-full w-4/5" />
                  <div className="h-2.5 bg-primary/20 rounded-full w-3/4" />
                  <div className="h-2.5 bg-muted rounded-full w-full" />
                  <div className="h-2.5 bg-muted rounded-full w-2/3" />
                </div>
                {/* Risk badge row */}
                <div className="flex items-center gap-3 pt-3 border-t border-border">
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-yellow-500/10 text-yellow-600 border border-yellow-500/20">
                    ⚠ MEDIUM RISK
                  </span>
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-primary/10 text-primary border border-primary/20">
                    3 Key Clauses Found
                  </span>
                </div>
              </div>
            </div>

            {/* Floating chat bubble (top-right) */}
            <div
              className={`absolute top-4 right-4 bg-card border border-border rounded-xl shadow-xl p-4 w-56 ${
                mounted ? "animate-float-reverse" : ""
              }`}
            >
              <div className="flex items-center gap-2 mb-2">
                <div className="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center text-xs">🤖</div>
                <span className="text-xs font-semibold text-foreground">AI Assistant</span>
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed">
                "Clause 4.2 contains a non-compete restriction that may limit your future employment..."
              </p>
            </div>

            {/* Floating shield badge (bottom-left) */}
            <div
              className={`absolute bottom-2 -left-4 bg-card border border-border rounded-xl shadow-lg p-3 flex items-center gap-2 ${
                mounted ? "animate-float delay-300" : ""
              }`}
            >
              <Shield className="w-5 h-5 text-green-500" />
              <div>
                <p className="text-[10px] font-bold text-foreground">Risk Detected</p>
                <p className="text-[10px] text-muted-foreground">2 risky clauses</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Stats Strip ─────────────────────────────────────────────── */}
      <section className="border-y border-border bg-muted/30">
        <div className="max-w-7xl mx-auto px-6 py-10 grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
          {stats.map((stat, i) => (
            <div key={i} className="space-y-1">
              <p
                className="text-3xl font-serif font-bold"
                style={{
                  background: "linear-gradient(135deg, oklch(0.555 0.163 49), oklch(0.75 0.14 70))",
                  WebkitBackgroundClip: "text",
                  WebkitTextFillColor: "transparent",
                  backgroundClip: "text",
                }}
              >
                {stat.value}
              </p>
              <p className="text-xs text-muted-foreground font-medium uppercase tracking-widest">{stat.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Features Section ────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-6 py-24">
        <div className="text-center mb-16">
          <p className="text-primary text-sm font-semibold uppercase tracking-widest mb-3">Why LegalEase?</p>
          <h2 className="text-4xl font-serif font-bold text-foreground">
            Everything you need for legal clarity
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[
            {
              icon: <FileText className="w-6 h-6 text-primary" />,
              title: t.aiAnalysis,
              desc: t.aiAnalysisDesc,
              gradient: "from-primary/10 to-primary/5",
            },
            {
              icon: <MessageSquare className="w-6 h-6 text-primary" />,
              title: t.translation,
              desc: t.translationDesc,
              gradient: "from-primary/10 to-primary/5",
            },
            {
              icon: <Users className="w-6 h-6 text-primary" />,
              title: t.consultation,
              desc: t.consultationDesc,
              gradient: "from-primary/10 to-primary/5",
            },
          ].map((feature, i) => (
            <div
              key={i}
              className={`group relative overflow-hidden rounded-2xl border border-border bg-card p-8 hover:border-primary/40 hover:shadow-xl hover:shadow-primary/5 transition-all duration-300 ${
                mounted ? `animate-slide-up delay-${(i + 1) * 200}` : "opacity-0"
              }`}
            >
              {/* Background gradient on hover */}
              <div className={`absolute inset-0 bg-gradient-to-br ${feature.gradient} opacity-0 group-hover:opacity-100 transition-opacity duration-300`} />

              <div className="relative">
                <div className="w-12 h-12 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center mb-5 group-hover:scale-110 transition-transform duration-300">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-serif font-bold text-foreground mb-3">{feature.title}</h3>
                <p className="text-muted-foreground text-sm leading-relaxed">{feature.desc}</p>
              </div>

              {/* Corner accent */}
              <div className="absolute top-0 right-0 w-24 h-24 bg-primary/5 rounded-bl-full opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA Banner ──────────────────────────────────────────────── */}
      <section className="relative overflow-hidden border-t border-border">
        <div
          className="absolute inset-0 animate-gradient"
          style={{
            background: "linear-gradient(135deg, oklch(0.555 0.163 49) 0%, oklch(0.65 0.15 60) 50%, oklch(0.555 0.163 49) 100%)",
          }}
        />
        <div className="relative max-w-4xl mx-auto px-6 py-20 text-center">
          <h2 className="text-4xl font-serif font-bold text-white mb-4">
            Start analyzing your documents today
          </h2>
          <p className="text-white/80 text-lg mb-8 font-light">
            Join thousands of Nepali citizens and businesses who trust LegalEase for legal clarity.
          </p>
          <SignedOut>
            <Link href="/sign-up" className="group inline-flex items-center gap-2 bg-white text-primary font-bold px-8 py-4 rounded-xl shadow-2xl hover:bg-white/90 transition-all duration-200 active:scale-95 text-base">
              {t.getStarted}
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
          </SignedOut>
          <SignedIn>
            <Link href="/dashboard">
              <button className="group inline-flex items-center gap-2 bg-white text-primary font-bold px-8 py-4 rounded-xl shadow-2xl hover:bg-white/90 transition-all duration-200 text-base">
                {t.goToDashboard}
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </button>
            </Link>
          </SignedIn>
        </div>
      </section>

      {/* ── Footer ──────────────────────────────────────────────────── */}
      <footer className="border-t border-border py-8 px-6">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Scale className="w-4 h-4 text-primary" />
            <span className="text-sm font-serif font-bold text-foreground">LegalEase Nepal</span>
          </div>
          <p className="text-xs text-muted-foreground">
            © {new Date().getFullYear()} LegalEase Nepal. All rights reserved.
          </p>
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <a href="#" className="hover:text-primary transition-colors">Privacy</a>
            <a href="#" className="hover:text-primary transition-colors">Terms</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
