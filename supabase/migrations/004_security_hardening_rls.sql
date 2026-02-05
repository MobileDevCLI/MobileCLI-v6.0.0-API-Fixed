-- MobileCLI Pro - Security Hardening: Fix RLS Policies
-- Run this in Supabase SQL Editor: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql
--
-- PROBLEM: "Webhook can manage subscriptions" policy grants ALL operations
-- to the public/anon role (qual=true). Anyone with the anon key can
-- read/write/delete ALL subscriptions.
--
-- FIX: Drop dangerous policies. Webhooks use service_role key which
-- bypasses RLS entirely - no replacement policy needed.
-- Enable RLS on all unprotected tables.

-- ============================================================================
-- 1. FIX SUBSCRIPTIONS TABLE: Remove dangerous public-access policies
-- ============================================================================

-- Drop the overly permissive policies (these apply to anon/public role)
DROP POLICY IF EXISTS "Webhook can manage subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Service role can insert subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Service role can update subscriptions" ON public.subscriptions;

-- Webhooks use SUPABASE_SERVICE_ROLE_KEY which bypasses RLS entirely.
-- No replacement policy needed for webhooks.

-- Ensure users can read their own subscription (may already exist)
DROP POLICY IF EXISTS "Users can view own subscription" ON public.subscriptions;
CREATE POLICY "Users can view own subscription"
    ON public.subscriptions
    FOR SELECT
    USING (auth.uid() = user_id);

-- ============================================================================
-- 2. ENABLE RLS ON UNPROTECTED TABLES
-- ============================================================================

-- payment_history: RLS OFF -> ON
ALTER TABLE public.payment_history ENABLE ROW LEVEL SECURITY;

-- webhook_logs: RLS OFF -> ON (no public policies - service_role only)
ALTER TABLE public.webhook_logs ENABLE ROW LEVEL SECURITY;

-- admin_users: RLS OFF -> ON (no public policies - service_role only)
ALTER TABLE public.admin_users ENABLE ROW LEVEL SECURITY;

-- email_logs: RLS OFF -> ON (no public policies - service_role only)
ALTER TABLE public.email_logs ENABLE ROW LEVEL SECURITY;

-- user_profiles: RLS OFF -> ON
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- 3. ADD USER-ONLY READ POLICIES
-- ============================================================================

-- Users can view their own payment history
DROP POLICY IF EXISTS "Users can view own payments" ON public.payment_history;
CREATE POLICY "Users can view own payments"
    ON public.payment_history
    FOR SELECT
    USING (auth.uid() = user_id);

-- Users can view their own profile
DROP POLICY IF EXISTS "Users can view own profile" ON public.user_profiles;
CREATE POLICY "Users can view own profile"
    ON public.user_profiles
    FOR SELECT
    USING (auth.uid() = user_id);

-- webhook_logs, admin_users, email_logs: NO public policies.
-- Only accessible via service_role key (admin/backend use).

-- ============================================================================
-- 4. VERIFY
-- ============================================================================

-- Check all policies:
-- SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
-- FROM pg_policies
-- WHERE schemaname = 'public'
-- ORDER BY tablename, policyname;

-- Check RLS is enabled:
-- SELECT tablename, rowsecurity
-- FROM pg_tables
-- WHERE schemaname = 'public'
-- ORDER BY tablename;
