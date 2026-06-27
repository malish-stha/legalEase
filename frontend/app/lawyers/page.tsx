"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useAuth, useUser, UserButton } from "@clerk/nextjs";
import axios from "axios";
import {
  Users, Search, MapPin, Award, Star, Clock, ChevronRight,
  LayoutDashboard, FileStack, Scale, ShieldCheck, DollarSign
} from "lucide-react";
import Link from "next/link";
import { useSelector } from "react-redux";
import { RootState } from "../../store/store";
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

const navItems = [
  { href: "/dashboard", icon: LayoutDashboard, labelKey: "dashboard" },
  { href: "/templates", icon: FileStack, labelKey: "templates" },
  { href: "/lawyers", icon: Users, labelKey: "findLawyersSidebar" },
];

export default function LawyersDirectory() {
  const { getToken } = useAuth();
  const { user } = useUser();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [lawyers, setLawyers] = useState<Lawyer[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedSpecialization, setSelectedSpecialization] = useState("");
  const [selectedLocation, setSelectedLocation] = useState("");

  const fetchLawyers = useCallback(async () => {
    try {
      setLoading(true);
      const token = await getToken();
      if (!token) return;

      const params: any = {};
      if (selectedSpecialization) params.specialization = selectedSpecialization;
      if (selectedLocation) params.location = selectedLocation;

      const response = await axios.get(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/lawyers`,
        {
          headers: { Authorization: `Bearer ${token}` },
          params
        }
      );
      setLawyers(response.data);
    } catch (err) {
      console.error("Failed to fetch lawyers", err);
    } finally {
      setLoading(false);
    }
  }, [getToken, selectedSpecialization, selectedLocation]);

  useEffect(() => {
    fetchLawyers();
  }, [fetchLawyers]);

  // Client-side filtering by name/bio as well
  const filteredLawyers = lawyers.filter(lawyer => {
    const query = searchQuery.toLowerCase();
    return (
      lawyer.name.toLowerCase().includes(query) ||
      lawyer.bio.toLowerCase().includes(query) ||
      lawyer.location.toLowerCase().includes(query)
    );
  });

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
        {/* Top bar */}
        <header className="border-b border-border px-6 py-4 flex items-center justify-between md:px-8 bg-background/80 backdrop-blur sticky top-0 z-10">
          <div className="flex items-center gap-3">
            <Users className="w-5 h-5 text-primary" />
            <h1 className="text-xl font-serif font-bold text-foreground">{t.lawyersTitle}</h1>
          </div>
          <div className="flex items-center gap-3 md:hidden">
            <LanguageToggler />
            <UserButton afterSignOutUrl="/" />
          </div>
        </header>

        <div className="p-6 md:p-8 space-y-8 max-w-7xl mx-auto">
          {/* Subtitle / Intro */}
          <div className="space-y-2">
            <p className="text-muted-foreground max-w-2xl">
              {t.lawyersSub}
            </p>
          </div>

          {/* ── Search & Filter Panel ── */}
          <div className="bg-card border border-border rounded-2xl p-6 shadow-sm space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Search input */}
              <div className="relative">
                <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <input
                  type="text"
                  placeholder={t.searchPlaceholder}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 bg-background border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              {/* Specialization selector */}
              <div className="relative">
                <select
                  value={selectedSpecialization}
                  onChange={(e) => setSelectedSpecialization(e.target.value)}
                  className="w-full px-4 py-2.5 bg-background border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all appearance-none cursor-pointer"
                >
                  <option value="">{t.specializationAll}</option>
                  <option value="CIVIL">Civil Law (दीवानी कानून)</option>
                  <option value="LABOUR">Labour Law (श्रम कानून)</option>
                  <option value="CORPORATE">Corporate Law (कर्पोरेट कानून)</option>
                </select>
                <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none border-l border-border pl-2">
                  <Award className="w-4 h-4 text-muted-foreground" />
                </div>
              </div>

              {/* Location selector */}
              <div className="relative">
                <select
                  value={selectedLocation}
                  onChange={(e) => setSelectedLocation(e.target.value)}
                  className="w-full px-4 py-2.5 bg-background border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all appearance-none cursor-pointer"
                >
                  <option value="">{t.locationAll}</option>
                  <option value="Kathmandu">Kathmandu (काठमाडौं)</option>
                  <option value="Lalitpur">Lalitpur (ललितपुर)</option>
                  <option value="Bhaktapur">Bhaktapur (भक्तपुर)</option>
                  <option value="Pokhara">Pokhara (पोखरा)</option>
                </select>
                <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none border-l border-border pl-2">
                  <MapPin className="w-4 h-4 text-muted-foreground" />
                </div>
              </div>
            </div>
          </div>

          {/* ── Lawyers Grid ── */}
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3 text-muted-foreground">
              <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
              <p className="text-sm">{t.loading}</p>
            </div>
          ) : filteredLawyers.length === 0 ? (
            <div className="text-center py-20 rounded-2xl border border-dashed border-border bg-card/30">
              <Users className="h-12 w-12 mx-auto mb-3 text-muted-foreground/30" />
              <p className="text-muted-foreground text-sm font-semibold">{t.noLawyersFound}</p>
              <p className="text-muted-foreground/60 text-xs mt-1">Try modifying your filter settings</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
              {filteredLawyers.map((lawyer) => (
                <div
                  key={lawyer.id}
                  className="group relative bg-card border border-border rounded-2xl p-6 hover:border-primary/30 hover:shadow-xl hover:shadow-primary/5 transition-all duration-300 flex flex-col justify-between overflow-hidden"
                >
                  {/* Glass highlight effect */}
                  <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"
                    style={{
                      background: "radial-gradient(ellipse at 50% -20%, oklch(0.555 0.163 49 / 0.08) 0%, transparent 60%)"
                    }}
                  />

                  <div className="space-y-4 relative">
                    {/* Header: Avatar, Name & Verification */}
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-primary/10 border border-primary/20 flex items-center justify-center font-bold text-primary text-lg">
                          {lawyer.name.split(" ").map(n => n[0]).join("")}
                        </div>
                        <div>
                          <h3 className="font-bold text-foreground text-base group-hover:text-primary transition-colors">
                            {lawyer.name}
                          </h3>
                          <div className="flex items-center gap-1.5 mt-0.5">
                            <span className="text-[10px] uppercase font-bold tracking-wider text-muted-foreground px-2 py-0.5 bg-muted rounded-md border border-border">
                              {lawyer.specialization} LAW
                            </span>
                          </div>
                        </div>
                      </div>
                      
                      {lawyer.isVerified && (
                        <div className="flex items-center gap-1 text-[10px] text-green-500 font-bold bg-green-500/10 border border-green-500/20 px-2 py-1 rounded-full shrink-0">
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span className="hidden sm:inline">{t.verifiedLawyer}</span>
                        </div>
                      )}
                    </div>

                    {/* Stats: Exp, Location, Rating */}
                    <div className="grid grid-cols-3 gap-2 py-2 border-y border-border/60 text-xs">
                      <div className="flex flex-col items-center justify-center p-2 rounded-lg bg-muted/40">
                        <Award className="w-3.5 h-3.5 text-primary mb-1" />
                        <span className="font-semibold text-foreground">{lawyer.experienceYears} Yrs</span>
                        <span className="text-[9px] text-muted-foreground">Experience</span>
                      </div>
                      <div className="flex flex-col items-center justify-center p-2 rounded-lg bg-muted/40">
                        <MapPin className="w-3.5 h-3.5 text-primary mb-1" />
                        <span className="font-semibold text-foreground truncate max-w-full">{lawyer.location}</span>
                        <span className="text-[9px] text-muted-foreground">Location</span>
                      </div>
                      <div className="flex flex-col items-center justify-center p-2 rounded-lg bg-muted/40">
                        <Star className="w-3.5 h-3.5 text-yellow-500 fill-yellow-500 mb-1" />
                        <span className="font-semibold text-foreground">{lawyer.rating.toFixed(1)}</span>
                        <span className="text-[9px] text-muted-foreground">Rating</span>
                      </div>
                    </div>

                    {/* Bio */}
                    <p className="text-xs text-muted-foreground line-clamp-3 leading-relaxed min-h-[54px]">
                      {lawyer.bio}
                    </p>
                  </div>

                  {/* Hourly Rate and Book button */}
                  <div className="pt-4 mt-4 border-t border-border flex items-center justify-between gap-4 relative">
                    <div className="flex flex-col">
                      <span className="text-[10px] text-muted-foreground uppercase font-bold tracking-wider">Rate</span>
                      <span className="text-sm font-bold text-foreground flex items-center">
                        Rs. {lawyer.hourlyRate.toLocaleString()} <span className="text-[10px] text-muted-foreground font-normal ml-1">{t.hourlyRateVal}</span>
                      </span>
                    </div>

                    <Link
                      href={`/lawyers/${lawyer.id}`}
                      className="group/btn flex items-center gap-1.5 text-xs font-semibold bg-primary hover:bg-primary/90 text-primary-foreground px-4 py-2.5 rounded-xl transition-all shadow-md shadow-primary/20 hover:shadow-primary/40 shrink-0"
                    >
                      {t.bookNow}
                      <ChevronRight className="w-3.5 h-3.5 group-hover/btn:translate-x-0.5 transition-transform" />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
