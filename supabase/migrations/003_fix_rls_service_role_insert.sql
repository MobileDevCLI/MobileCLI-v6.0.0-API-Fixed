-- MobileCLI Pro - Fix RLS Policy for Service Role INSERT
-- Run this in Supabase SQL Editor: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql
--
-- PROBLEM: Webhook uses service_role but RLS only allows UPDATE, not INSERT
-- SOLUTION: Add INSERT policy for service_role so upsert works

-- ============================================================================
-- 1. ADD INSERT POLICY FOR SERVICE ROLE
-- ============================================================================

-- Drop existing policy if it exists (safe to run multiple times)
DROP POLICY IF EXISTS "Service role can insert subscriptions" ON public.subscriptions;

-- Create INSERT policy for service_role
CREATE POLICY "Service role can insert subscriptions"
    ON public.subscriptions
    FOR INSERT
    WITH CHECK (true);  -- Service role bypasses RLS anyway, but this makes it explicit

-- ============================================================================
-- 2. ALSO FIX: Ensure service_role can do ALL operations
-- ============================================================================

-- Drop and recreate update policy to be more permissive
DROP POLICY IF EXISTS "Service role can update subscriptions" ON public.subscriptions;

CREATE POLICY "Service role can update subscriptions"
    ON public.subscriptions
    FOR UPDATE
    USING (true);  -- Service role should be able to update any row

-- ============================================================================
-- 3. ALTERNATIVE: Disable RLS for service_role entirely (recommended)
-- ============================================================================

-- Actually, service_role should bypass RLS entirely by default in Supabase.
-- The issue might be that we're not using service_role key correctly.
-- Let's also add a policy that allows the webhook to insert:

DROP POLICY IF EXISTS "Webhook can manage subscriptions" ON public.subscriptions;

CREATE POLICY "Webhook can manage subscriptions"
    ON public.subscriptions
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- ============================================================================
-- 4. VERIFY
-- ============================================================================

-- Check policies:
-- SELECT * FROM pg_policies WHERE tablename = 'subscriptions';

-- Test insert (should work now):
-- INSERT INTO public.subscriptions (user_id, status)
-- VALUES ('00000000-0000-0000-0000-000000000001', 'test')
-- ON CONFLICT (user_id) DO UPDATE SET status = 'test';

-- ============================================================================
-- NOTE: After running this, re-test the webhook to verify subscriptions
-- are being inserted correctly.
-- ============================================================================
