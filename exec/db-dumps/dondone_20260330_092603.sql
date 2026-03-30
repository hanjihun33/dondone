--
-- PostgreSQL database dump
--

\restrict GUR3T3FyCgweW4LLjM30ebEAyleoYPImQWn6gff7uNeXjjgy7z5OCHsU1n5YMH4

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY test.workplaces DROP CONSTRAINT IF EXISTS workplaces_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_records DROP CONSTRAINT IF EXISTS work_proof_records_workplace_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_records DROP CONSTRAINT IF EXISTS work_proof_records_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_records DROP CONSTRAINT IF EXISTS work_proof_records_contract_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modifications DROP CONSTRAINT IF EXISTS work_proof_modifications_reviewed_by_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modifications DROP CONSTRAINT IF EXISTS work_proof_modifications_record_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modifications DROP CONSTRAINT IF EXISTS work_proof_modifications_modified_by_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modification_attachments DROP CONSTRAINT IF EXISTS work_proof_modification_attachments_modification_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modification_attachments DROP CONSTRAINT IF EXISTS work_proof_modification_attachments_attachment_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_attachments DROP CONSTRAINT IF EXISTS work_proof_attachments_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.work_contracts DROP CONSTRAINT IF EXISTS work_contracts_workplace_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_verifications DROP CONSTRAINT IF EXISTS wage_verifications_workplace_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_verifications DROP CONSTRAINT IF EXISTS wage_verifications_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_verifications DROP CONSTRAINT IF EXISTS wage_verifications_contract_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_verification_record_links DROP CONSTRAINT IF EXISTS wage_verification_record_links_work_proof_record_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_verification_record_links DROP CONSTRAINT IF EXISTS wage_verification_record_links_verification_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_verification_cause_snapshots DROP CONSTRAINT IF EXISTS wage_verification_cause_snapshots_verification_id_fkey;
ALTER TABLE IF EXISTS ONLY test.wage_deposits DROP CONSTRAINT IF EXISTS wage_deposits_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.vault_ledger_entries DROP CONSTRAINT IF EXISTS vault_ledger_entries_account_id_fkey;
ALTER TABLE IF EXISTS ONLY test.vault_accounts DROP CONSTRAINT IF EXISTS vault_accounts_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.safepay_transfer_checks DROP CONSTRAINT IF EXISTS safepay_transfer_checks_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.safepay_transfer_checks DROP CONSTRAINT IF EXISTS safepay_transfer_checks_recipient_id_fkey;
ALTER TABLE IF EXISTS ONLY test.safepay_transfer_check_reasons DROP CONSTRAINT IF EXISTS safepay_transfer_check_reasons_safepay_check_id_fkey;
ALTER TABLE IF EXISTS ONLY test.remittance_transfers DROP CONSTRAINT IF EXISTS remittance_transfers_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.remittance_transfers DROP CONSTRAINT IF EXISTS remittance_transfers_safepay_check_id_fkey;
ALTER TABLE IF EXISTS ONLY test.remittance_transfers DROP CONSTRAINT IF EXISTS remittance_transfers_recipient_id_fkey;
ALTER TABLE IF EXISTS ONLY test.remittance_transfer_events DROP CONSTRAINT IF EXISTS remittance_transfer_events_transfer_id_fkey;
ALTER TABLE IF EXISTS ONLY test.remittance_recipients DROP CONSTRAINT IF EXISTS remittance_recipients_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.documents DROP CONSTRAINT IF EXISTS documents_workplace_id_fkey;
ALTER TABLE IF EXISTS ONLY test.documents DROP CONSTRAINT IF EXISTS documents_wage_verification_id_fkey;
ALTER TABLE IF EXISTS ONLY test.documents DROP CONSTRAINT IF EXISTS documents_transfer_id_fkey;
ALTER TABLE IF EXISTS ONLY test.documents DROP CONSTRAINT IF EXISTS documents_requested_by_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparations DROP CONSTRAINT IF EXISTS claim_preparations_wage_verification_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparations DROP CONSTRAINT IF EXISTS claim_preparations_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparations DROP CONSTRAINT IF EXISTS claim_preparations_claim_kit_document_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_route_snapshots DROP CONSTRAINT IF EXISTS claim_preparation_route_snapshots_preparation_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_document_links DROP CONSTRAINT IF EXISTS claim_preparation_document_links_preparation_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_document_links DROP CONSTRAINT IF EXISTS claim_preparation_document_links_document_id_fkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_checklist_items DROP CONSTRAINT IF EXISTS claim_preparation_checklist_items_preparation_id_fkey;
ALTER TABLE IF EXISTS ONLY test.async_job_attempts DROP CONSTRAINT IF EXISTS async_job_attempts_job_id_fkey;
ALTER TABLE IF EXISTS ONLY test.advance_requests DROP CONSTRAINT IF EXISTS advance_requests_workplace_id_fkey;
ALTER TABLE IF EXISTS ONLY test.advance_requests DROP CONSTRAINT IF EXISTS advance_requests_user_id_fkey;
ALTER TABLE IF EXISTS ONLY test.advance_requests DROP CONSTRAINT IF EXISTS advance_requests_contract_id_fkey;
ALTER TABLE IF EXISTS ONLY public.wage_deposits DROP CONSTRAINT IF EXISTS fkted40fqr2fx1v9et0l83hol9l;
ALTER TABLE IF EXISTS ONLY public.work_contracts DROP CONSTRAINT IF EXISTS fkrb33n670qvcxrtc43jf4pqb40;
ALTER TABLE IF EXISTS ONLY public.wage_verification_record_ids DROP CONSTRAINT IF EXISTS fkpdmbl22gfktluyostbdnfinrw;
ALTER TABLE IF EXISTS ONLY public.wage_verifications DROP CONSTRAINT IF EXISTS fko36svd7f53kgd19q0tw69kqbw;
ALTER TABLE IF EXISTS ONLY public.correction_requests DROP CONSTRAINT IF EXISTS fko0btpinq38ohvrlquvkiw7gsg;
ALTER TABLE IF EXISTS ONLY public.work_proofs DROP CONSTRAINT IF EXISTS fkmseqnn81xfiv7kaijg9lt4tua;
ALTER TABLE IF EXISTS ONLY public.advance_requests DROP CONSTRAINT IF EXISTS fkkty5fhc2d4u0y34bq4wjt72i2;
ALTER TABLE IF EXISTS ONLY public.correction_decision_audits DROP CONSTRAINT IF EXISTS fkkoyrv1abepkrcwdra32vuul7i;
ALTER TABLE IF EXISTS ONLY public.work_proofs DROP CONSTRAINT IF EXISTS fkjggnlxif2tfny4w22l7cociu0;
ALTER TABLE IF EXISTS ONLY public.wage_verification_possible_causes DROP CONSTRAINT IF EXISTS fkigpoupf5l1t1uukneb4qtodgy;
ALTER TABLE IF EXISTS ONLY public.workplaces DROP CONSTRAINT IF EXISTS fkhumiu2ff1qdapmuetejo5e9fc;
ALTER TABLE IF EXISTS ONLY public.claim_preparations DROP CONSTRAINT IF EXISTS fkhpoyyrdlfdtyixkbbsk9lua7p;
ALTER TABLE IF EXISTS ONLY public.advance_requests DROP CONSTRAINT IF EXISTS fkdalb49lwnrdbcy52qvkxnkwrc;
ALTER TABLE IF EXISTS ONLY public.advance_requests DROP CONSTRAINT IF EXISTS fkb2wp8riqhpu3rhqlp5ewmaylk;
ALTER TABLE IF EXISTS ONLY public.document_generation_requests DROP CONSTRAINT IF EXISTS fkaqbo0hf3pywnsrg2fladl7ojf;
ALTER TABLE IF EXISTS ONLY public.vault_yield_logs DROP CONSTRAINT IF EXISTS fk_vault_yield_logs_user;
ALTER TABLE IF EXISTS ONLY public.vault_yield_logs DROP CONSTRAINT IF EXISTS fk_vault_yield_logs_position;
ALTER TABLE IF EXISTS ONLY public.user_wallets DROP CONSTRAINT IF EXISTS fk_user_wallets_user;
ALTER TABLE IF EXISTS ONLY public.transfers DROP CONSTRAINT IF EXISTS fk_transfers_user;
ALTER TABLE IF EXISTS ONLY public.transfers DROP CONSTRAINT IF EXISTS fk_transfers_recipient;
ALTER TABLE IF EXISTS ONLY public.recipients DROP CONSTRAINT IF EXISTS fk_recipients_user;
ALTER TABLE IF EXISTS ONLY public.recipients DROP CONSTRAINT IF EXISTS fk_recipients_target_user;
ALTER TABLE IF EXISTS ONLY public.advance_payouts DROP CONSTRAINT IF EXISTS fk_advance_payouts_user;
ALTER TABLE IF EXISTS ONLY public.advance_payouts DROP CONSTRAINT IF EXISTS fk_advance_payouts_advance_request;
ALTER TABLE IF EXISTS ONLY public.work_proof_audit_logs DROP CONSTRAINT IF EXISTS fk824q9bqldq51go28gx27eaiie;
ALTER TABLE IF EXISTS ONLY public.work_proofs DROP CONSTRAINT IF EXISTS fk3rodpxe6g6d55ackcg28i4d43;
DROP TRIGGER IF EXISTS trg_workplaces_set_updated_at ON test.workplaces;
DROP TRIGGER IF EXISTS trg_work_proof_records_set_updated_at ON test.work_proof_records;
DROP TRIGGER IF EXISTS trg_work_proof_modifications_set_updated_at ON test.work_proof_modifications;
DROP TRIGGER IF EXISTS trg_work_contracts_set_updated_at ON test.work_contracts;
DROP TRIGGER IF EXISTS trg_wage_verifications_set_updated_at ON test.wage_verifications;
DROP TRIGGER IF EXISTS trg_wage_deposits_set_updated_at ON test.wage_deposits;
DROP TRIGGER IF EXISTS trg_vault_accounts_set_updated_at ON test.vault_accounts;
DROP TRIGGER IF EXISTS trg_users_set_updated_at ON test.users;
DROP TRIGGER IF EXISTS trg_remittance_transfers_set_updated_at ON test.remittance_transfers;
DROP TRIGGER IF EXISTS trg_remittance_recipients_set_updated_at ON test.remittance_recipients;
DROP TRIGGER IF EXISTS trg_documents_set_updated_at ON test.documents;
DROP TRIGGER IF EXISTS trg_claim_routes_set_updated_at ON test.claim_routes;
DROP TRIGGER IF EXISTS trg_claim_preparations_set_updated_at ON test.claim_preparations;
DROP TRIGGER IF EXISTS trg_async_jobs_set_updated_at ON test.async_jobs;
DROP TRIGGER IF EXISTS trg_advance_requests_set_updated_at ON test.advance_requests;
DROP INDEX IF EXISTS test.uk_work_proof_records_user_work_date;
DROP INDEX IF EXISTS test.uk_work_proof_records_open_per_user;
DROP INDEX IF EXISTS test.uk_work_proof_attachments_storage_object_key;
DROP INDEX IF EXISTS test.uk_work_contracts_active_per_workplace;
DROP INDEX IF EXISTS test.uk_vault_accounts_user_token;
DROP INDEX IF EXISTS test.uk_users_email_lower;
DROP INDEX IF EXISTS test.uk_remittance_transfers_user_idempotency;
DROP INDEX IF EXISTS test.uk_remittance_transfers_tx_hash;
DROP INDEX IF EXISTS test.uk_remittance_transfers_safepay_check_id;
DROP INDEX IF EXISTS test.uk_remittance_transfers_request_id;
DROP INDEX IF EXISTS test.uk_remittance_recipients_user_wallet;
DROP INDEX IF EXISTS test.uk_documents_user_type_idempotency;
DROP INDEX IF EXISTS test.uk_documents_transfer_receipt_per_transfer;
DROP INDEX IF EXISTS test.uk_documents_request_id;
DROP INDEX IF EXISTS test.uk_claim_routes_locale_channel_sort;
DROP INDEX IF EXISTS test.uk_claim_preparations_user_idempotency;
DROP INDEX IF EXISTS test.uk_async_jobs_dedupe_key;
DROP INDEX IF EXISTS test.uk_advance_requests_user_idempotency;
DROP INDEX IF EXISTS test.uk_advance_requests_active_month;
DROP INDEX IF EXISTS test.idx_workplaces_user_created_at;
DROP INDEX IF EXISTS test.idx_work_proof_records_user_workplace_work_date;
DROP INDEX IF EXISTS test.idx_work_proof_records_user_work_date;
DROP INDEX IF EXISTS test.idx_work_proof_modifications_record_created_at;
DROP INDEX IF EXISTS test.idx_work_proof_modification_attachments_attachment_id;
DROP INDEX IF EXISTS test.idx_work_proof_attachments_user_uploaded_at;
DROP INDEX IF EXISTS test.idx_work_contracts_workplace_effective_from;
DROP INDEX IF EXISTS test.idx_wage_verifications_user_month_start_workplace;
DROP INDEX IF EXISTS test.idx_wage_verification_record_links_record_id;
DROP INDEX IF EXISTS test.idx_wage_deposits_user_month_start;
DROP INDEX IF EXISTS test.idx_vault_ledger_entries_account_created_at;
DROP INDEX IF EXISTS test.idx_safepay_transfer_checks_user_checked_at;
DROP INDEX IF EXISTS test.idx_remittance_transfers_user_status_created_at;
DROP INDEX IF EXISTS test.idx_remittance_transfers_user_created_at;
DROP INDEX IF EXISTS test.idx_remittance_transfer_events_transfer_created_at;
DROP INDEX IF EXISTS test.idx_remittance_recipients_user_favorite_created;
DROP INDEX IF EXISTS test.idx_documents_wage_verification_id;
DROP INDEX IF EXISTS test.idx_documents_user_type_updated_at;
DROP INDEX IF EXISTS test.idx_documents_user_status_updated_at;
DROP INDEX IF EXISTS test.idx_documents_user_month_start;
DROP INDEX IF EXISTS test.idx_claim_routes_locale_active_sort;
DROP INDEX IF EXISTS test.idx_claim_preparations_verification_created_at;
DROP INDEX IF EXISTS test.idx_claim_preparations_user_created_at;
DROP INDEX IF EXISTS test.idx_async_jobs_status_run_after;
DROP INDEX IF EXISTS test.idx_async_jobs_resource_created_at;
DROP INDEX IF EXISTS test.idx_async_jobs_queue_status_run_after;
DROP INDEX IF EXISTS test.idx_async_jobs_concurrency_key;
DROP INDEX IF EXISTS test.idx_async_job_attempts_job_started_at;
DROP INDEX IF EXISTS test.idx_advance_requests_user_month_start;
DROP INDEX IF EXISTS public.idx_vault_transactions_user_status;
DROP INDEX IF EXISTS public.idx_vault_transactions_user_created_at;
DROP INDEX IF EXISTS public.idx_vault_transactions_status_updated_at;
DROP INDEX IF EXISTS public.idx_vault_positions_status_updated_at;
DROP INDEX IF EXISTS public.idx_user_wallets_funding_status_updated_at;
DROP INDEX IF EXISTS public.idx_transfers_user_status;
DROP INDEX IF EXISTS public.idx_transfers_user_created_at;
DROP INDEX IF EXISTS public.idx_transfers_status_updated_at;
DROP INDEX IF EXISTS public.idx_recipients_user_updated_at;
DROP INDEX IF EXISTS public.idx_recipients_target_user_id;
DROP INDEX IF EXISTS public.idx_jobs_status_run_at;
DROP INDEX IF EXISTS public.idx_jobs_reference_kind_id_type;
DROP INDEX IF EXISTS public.idx_advance_policies_enabled;
DROP INDEX IF EXISTS public.idx_advance_payouts_user_status;
DROP INDEX IF EXISTS public.idx_advance_payouts_user_created_at;
DROP INDEX IF EXISTS public.idx_advance_payouts_status_updated_at;
ALTER TABLE IF EXISTS ONLY test.workplaces DROP CONSTRAINT IF EXISTS workplaces_pkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_records DROP CONSTRAINT IF EXISTS work_proof_records_pkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modifications DROP CONSTRAINT IF EXISTS work_proof_modifications_pkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_modification_attachments DROP CONSTRAINT IF EXISTS work_proof_modification_attachments_pkey;
ALTER TABLE IF EXISTS ONLY test.work_proof_attachments DROP CONSTRAINT IF EXISTS work_proof_attachments_pkey;
ALTER TABLE IF EXISTS ONLY test.work_contracts DROP CONSTRAINT IF EXISTS work_contracts_pkey;
ALTER TABLE IF EXISTS ONLY test.wage_verifications DROP CONSTRAINT IF EXISTS wage_verifications_pkey;
ALTER TABLE IF EXISTS ONLY test.wage_verification_record_links DROP CONSTRAINT IF EXISTS wage_verification_record_links_pkey;
ALTER TABLE IF EXISTS ONLY test.wage_verification_cause_snapshots DROP CONSTRAINT IF EXISTS wage_verification_cause_snapshots_pkey;
ALTER TABLE IF EXISTS ONLY test.wage_deposits DROP CONSTRAINT IF EXISTS wage_deposits_pkey;
ALTER TABLE IF EXISTS ONLY test.vault_ledger_entries DROP CONSTRAINT IF EXISTS vault_ledger_entries_pkey;
ALTER TABLE IF EXISTS ONLY test.vault_accounts DROP CONSTRAINT IF EXISTS vault_accounts_pkey;
ALTER TABLE IF EXISTS ONLY test.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY test.wage_verification_record_links DROP CONSTRAINT IF EXISTS uk_wage_verification_record_links_record;
ALTER TABLE IF EXISTS ONLY test.wage_verification_cause_snapshots DROP CONSTRAINT IF EXISTS uk_wage_verification_cause_snapshots_order;
ALTER TABLE IF EXISTS ONLY test.safepay_transfer_check_reasons DROP CONSTRAINT IF EXISTS uk_safepay_transfer_check_reasons_sort;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_route_snapshots DROP CONSTRAINT IF EXISTS uk_claim_preparation_routes_sort;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_document_links DROP CONSTRAINT IF EXISTS uk_claim_preparation_document_links_sort;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_checklist_items DROP CONSTRAINT IF EXISTS uk_claim_preparation_checklist_sort;
ALTER TABLE IF EXISTS ONLY test.async_job_attempts DROP CONSTRAINT IF EXISTS uk_async_job_attempts_job_attempt;
ALTER TABLE IF EXISTS ONLY test.safepay_transfer_checks DROP CONSTRAINT IF EXISTS safepay_transfer_checks_pkey;
ALTER TABLE IF EXISTS ONLY test.safepay_transfer_check_reasons DROP CONSTRAINT IF EXISTS safepay_transfer_check_reasons_pkey;
ALTER TABLE IF EXISTS ONLY test.remittance_transfers DROP CONSTRAINT IF EXISTS remittance_transfers_pkey;
ALTER TABLE IF EXISTS ONLY test.remittance_transfer_events DROP CONSTRAINT IF EXISTS remittance_transfer_events_pkey;
ALTER TABLE IF EXISTS ONLY test.remittance_recipients DROP CONSTRAINT IF EXISTS remittance_recipients_pkey;
ALTER TABLE IF EXISTS ONLY test.work_contracts DROP CONSTRAINT IF EXISTS ex_work_contracts_no_overlap;
ALTER TABLE IF EXISTS ONLY test.documents DROP CONSTRAINT IF EXISTS documents_pkey;
ALTER TABLE IF EXISTS ONLY test.claim_routes DROP CONSTRAINT IF EXISTS claim_routes_pkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparations DROP CONSTRAINT IF EXISTS claim_preparations_pkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_route_snapshots DROP CONSTRAINT IF EXISTS claim_preparation_route_snapshots_pkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_document_links DROP CONSTRAINT IF EXISTS claim_preparation_document_links_pkey;
ALTER TABLE IF EXISTS ONLY test.claim_preparation_checklist_items DROP CONSTRAINT IF EXISTS claim_preparation_checklist_items_pkey;
ALTER TABLE IF EXISTS ONLY test.async_jobs DROP CONSTRAINT IF EXISTS async_jobs_pkey;
ALTER TABLE IF EXISTS ONLY test.async_job_attempts DROP CONSTRAINT IF EXISTS async_job_attempts_pkey;
ALTER TABLE IF EXISTS ONLY test.advance_requests DROP CONSTRAINT IF EXISTS advance_requests_pkey;
ALTER TABLE IF EXISTS ONLY public.workplaces DROP CONSTRAINT IF EXISTS workplaces_pkey;
ALTER TABLE IF EXISTS ONLY public.worker_registration_codes DROP CONSTRAINT IF EXISTS worker_registration_codes_pkey;
ALTER TABLE IF EXISTS ONLY public.work_proofs DROP CONSTRAINT IF EXISTS work_proofs_pkey;
ALTER TABLE IF EXISTS ONLY public.work_proof_audit_logs DROP CONSTRAINT IF EXISTS work_proof_audit_logs_pkey;
ALTER TABLE IF EXISTS ONLY public.work_contracts DROP CONSTRAINT IF EXISTS work_contracts_pkey;
ALTER TABLE IF EXISTS ONLY public.wage_verifications DROP CONSTRAINT IF EXISTS wage_verifications_pkey;
ALTER TABLE IF EXISTS ONLY public.wage_verification_record_ids DROP CONSTRAINT IF EXISTS wage_verification_record_ids_pkey;
ALTER TABLE IF EXISTS ONLY public.wage_verification_possible_causes DROP CONSTRAINT IF EXISTS wage_verification_possible_causes_pkey;
ALTER TABLE IF EXISTS ONLY public.wage_deposits DROP CONSTRAINT IF EXISTS wage_deposits_pkey;
ALTER TABLE IF EXISTS ONLY public.vault_yield_logs DROP CONSTRAINT IF EXISTS vault_yield_logs_pkey;
ALTER TABLE IF EXISTS ONLY public.vault_transactions DROP CONSTRAINT IF EXISTS vault_transactions_pkey;
ALTER TABLE IF EXISTS ONLY public.vault_positions DROP CONSTRAINT IF EXISTS vault_positions_pkey;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_phone_number_key;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE IF EXISTS ONLY public.user_wallets DROP CONSTRAINT IF EXISTS user_wallets_wallet_address_key;
ALTER TABLE IF EXISTS ONLY public.user_wallets DROP CONSTRAINT IF EXISTS user_wallets_pkey;
ALTER TABLE IF EXISTS ONLY public.worker_registration_codes DROP CONSTRAINT IF EXISTS uk_worker_registration_codes_code_hash;
ALTER TABLE IF EXISTS ONLY public.vault_transactions DROP CONSTRAINT IF EXISTS uk_vault_transactions_user_idempotency;
ALTER TABLE IF EXISTS ONLY public.vault_transactions DROP CONSTRAINT IF EXISTS uk_vault_transactions_tx_hash;
ALTER TABLE IF EXISTS ONLY public.vault_positions DROP CONSTRAINT IF EXISTS uk_vault_positions_user;
ALTER TABLE IF EXISTS ONLY public.transfers DROP CONSTRAINT IF EXISTS uk_transfers_user_idempotency;
ALTER TABLE IF EXISTS ONLY public.transfers DROP CONSTRAINT IF EXISTS uk_transfers_tx_hash;
ALTER TABLE IF EXISTS ONLY public.recipients DROP CONSTRAINT IF EXISTS uk_recipients_user_wallet;
ALTER TABLE IF EXISTS ONLY public.jobs DROP CONSTRAINT IF EXISTS uk_jobs_active_key;
ALTER TABLE IF EXISTS ONLY public.employer_signup_codes DROP CONSTRAINT IF EXISTS uk_employer_signup_codes_code_hash;
ALTER TABLE IF EXISTS ONLY public.employer_profiles DROP CONSTRAINT IF EXISTS uk_employer_profiles_account_id;
ALTER TABLE IF EXISTS ONLY public.employer_invitation_tokens DROP CONSTRAINT IF EXISTS uk_employer_invitation_tokens_token_hash;
ALTER TABLE IF EXISTS ONLY public.document_generation_requests DROP CONSTRAINT IF EXISTS uk_document_generation_requests_user_type_key;
ALTER TABLE IF EXISTS ONLY public.advance_requests DROP CONSTRAINT IF EXISTS uk_advance_requests_user_idempotency;
ALTER TABLE IF EXISTS ONLY public.advance_payouts DROP CONSTRAINT IF EXISTS uk_advance_payouts_user_idempotency;
ALTER TABLE IF EXISTS ONLY public.advance_payouts DROP CONSTRAINT IF EXISTS uk_advance_payouts_tx_hash;
ALTER TABLE IF EXISTS ONLY public.advance_payouts DROP CONSTRAINT IF EXISTS uk_advance_payouts_advance_request;
ALTER TABLE IF EXISTS ONLY public.transfers DROP CONSTRAINT IF EXISTS transfers_pkey;
ALTER TABLE IF EXISTS ONLY public.recipients DROP CONSTRAINT IF EXISTS recipients_pkey;
ALTER TABLE IF EXISTS ONLY public.jobs DROP CONSTRAINT IF EXISTS jobs_pkey;
ALTER TABLE IF EXISTS ONLY public.employment_memberships DROP CONSTRAINT IF EXISTS employment_memberships_pkey;
ALTER TABLE IF EXISTS ONLY public.employer_signup_codes DROP CONSTRAINT IF EXISTS employer_signup_codes_pkey;
ALTER TABLE IF EXISTS ONLY public.employer_profiles DROP CONSTRAINT IF EXISTS employer_profiles_pkey;
ALTER TABLE IF EXISTS ONLY public.employer_invitation_tokens DROP CONSTRAINT IF EXISTS employer_invitation_tokens_pkey;
ALTER TABLE IF EXISTS ONLY public.document_generation_requests DROP CONSTRAINT IF EXISTS document_generation_requests_request_id_key;
ALTER TABLE IF EXISTS ONLY public.document_generation_requests DROP CONSTRAINT IF EXISTS document_generation_requests_pkey;
ALTER TABLE IF EXISTS ONLY public.correction_requests DROP CONSTRAINT IF EXISTS correction_requests_pkey;
ALTER TABLE IF EXISTS ONLY public.correction_decision_audits DROP CONSTRAINT IF EXISTS correction_decision_audits_pkey;
ALTER TABLE IF EXISTS ONLY public.companies DROP CONSTRAINT IF EXISTS companies_pkey;
ALTER TABLE IF EXISTS ONLY public.companies DROP CONSTRAINT IF EXISTS companies_company_code_key;
ALTER TABLE IF EXISTS ONLY public.claim_preparations DROP CONSTRAINT IF EXISTS claim_preparations_pkey;
ALTER TABLE IF EXISTS ONLY public.advance_requests DROP CONSTRAINT IF EXISTS advance_requests_pkey;
ALTER TABLE IF EXISTS ONLY public.advance_policies DROP CONSTRAINT IF EXISTS advance_policies_pkey;
ALTER TABLE IF EXISTS ONLY public.advance_payouts DROP CONSTRAINT IF EXISTS advance_payouts_pkey;
SELECT pg_catalog.lo_unlink(oid) FROM pg_catalog.pg_largeobject_metadata WHERE oid = '18315';
SELECT pg_catalog.lo_unlink(oid) FROM pg_catalog.pg_largeobject_metadata WHERE oid = '18314';
SELECT pg_catalog.lo_unlink(oid) FROM pg_catalog.pg_largeobject_metadata WHERE oid = '18313';
SELECT pg_catalog.lo_unlink(oid) FROM pg_catalog.pg_largeobject_metadata WHERE oid = '18312';
ALTER TABLE IF EXISTS public.workplaces ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.worker_registration_codes ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.work_proofs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.work_proof_audit_logs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.work_contracts ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.wage_verifications ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.wage_deposits ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.vault_yield_logs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.vault_positions ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.users ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.jobs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.employment_memberships ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.employer_signup_codes ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.employer_profiles ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.employer_invitation_tokens ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.document_generation_requests ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.correction_requests ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.correction_decision_audits ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.companies ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.claim_preparations ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.advance_requests ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.advance_policies ALTER COLUMN advance_policy_id DROP DEFAULT;
DROP TABLE IF EXISTS test.workplaces;
DROP TABLE IF EXISTS test.work_proof_records;
DROP TABLE IF EXISTS test.work_proof_modifications;
DROP TABLE IF EXISTS test.work_proof_modification_attachments;
DROP TABLE IF EXISTS test.work_proof_attachments;
DROP TABLE IF EXISTS test.work_contracts;
DROP TABLE IF EXISTS test.wage_verifications;
DROP TABLE IF EXISTS test.wage_verification_record_links;
DROP TABLE IF EXISTS test.wage_verification_cause_snapshots;
DROP TABLE IF EXISTS test.wage_deposits;
DROP TABLE IF EXISTS test.vault_ledger_entries;
DROP TABLE IF EXISTS test.vault_accounts;
DROP TABLE IF EXISTS test.users;
DROP TABLE IF EXISTS test.safepay_transfer_checks;
DROP TABLE IF EXISTS test.safepay_transfer_check_reasons;
DROP TABLE IF EXISTS test.remittance_transfers;
DROP TABLE IF EXISTS test.remittance_transfer_events;
DROP TABLE IF EXISTS test.remittance_recipients;
DROP TABLE IF EXISTS test.documents;
DROP TABLE IF EXISTS test.claim_routes;
DROP TABLE IF EXISTS test.claim_preparations;
DROP TABLE IF EXISTS test.claim_preparation_route_snapshots;
DROP TABLE IF EXISTS test.claim_preparation_document_links;
DROP TABLE IF EXISTS test.claim_preparation_checklist_items;
DROP TABLE IF EXISTS test.async_jobs;
DROP TABLE IF EXISTS test.async_job_attempts;
DROP TABLE IF EXISTS test.advance_requests;
DROP SEQUENCE IF EXISTS public.workplaces_id_seq;
DROP TABLE IF EXISTS public.workplaces;
DROP SEQUENCE IF EXISTS public.worker_registration_codes_id_seq;
DROP TABLE IF EXISTS public.worker_registration_codes;
DROP SEQUENCE IF EXISTS public.work_proofs_id_seq;
DROP TABLE IF EXISTS public.work_proofs;
DROP SEQUENCE IF EXISTS public.work_proof_audit_logs_id_seq;
DROP TABLE IF EXISTS public.work_proof_audit_logs;
DROP SEQUENCE IF EXISTS public.work_contracts_id_seq;
DROP TABLE IF EXISTS public.work_contracts;
DROP SEQUENCE IF EXISTS public.wage_verifications_id_seq;
DROP TABLE IF EXISTS public.wage_verifications;
DROP TABLE IF EXISTS public.wage_verification_record_ids;
DROP TABLE IF EXISTS public.wage_verification_possible_causes;
DROP SEQUENCE IF EXISTS public.wage_deposits_id_seq;
DROP TABLE IF EXISTS public.wage_deposits;
DROP SEQUENCE IF EXISTS public.vault_yield_logs_id_seq;
DROP TABLE IF EXISTS public.vault_yield_logs;
DROP TABLE IF EXISTS public.vault_transactions;
DROP SEQUENCE IF EXISTS public.vault_positions_id_seq;
DROP TABLE IF EXISTS public.vault_positions;
DROP SEQUENCE IF EXISTS public.users_id_seq;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.user_wallets;
DROP TABLE IF EXISTS public.transfers;
DROP TABLE IF EXISTS public.recipients;
DROP SEQUENCE IF EXISTS public.jobs_id_seq;
DROP TABLE IF EXISTS public.jobs;
DROP SEQUENCE IF EXISTS public.employment_memberships_id_seq;
DROP TABLE IF EXISTS public.employment_memberships;
DROP SEQUENCE IF EXISTS public.employer_signup_codes_id_seq;
DROP TABLE IF EXISTS public.employer_signup_codes;
DROP SEQUENCE IF EXISTS public.employer_profiles_id_seq;
DROP TABLE IF EXISTS public.employer_profiles;
DROP SEQUENCE IF EXISTS public.employer_invitation_tokens_id_seq;
DROP TABLE IF EXISTS public.employer_invitation_tokens;
DROP SEQUENCE IF EXISTS public.document_generation_requests_id_seq;
DROP TABLE IF EXISTS public.document_generation_requests;
DROP SEQUENCE IF EXISTS public.correction_requests_id_seq;
DROP TABLE IF EXISTS public.correction_requests;
DROP SEQUENCE IF EXISTS public.correction_decision_audits_id_seq;
DROP TABLE IF EXISTS public.correction_decision_audits;
DROP SEQUENCE IF EXISTS public.companies_id_seq;
DROP TABLE IF EXISTS public.companies;
DROP SEQUENCE IF EXISTS public.claim_preparations_id_seq;
DROP TABLE IF EXISTS public.claim_preparations;
DROP SEQUENCE IF EXISTS public.advance_requests_id_seq;
DROP TABLE IF EXISTS public.advance_requests;
DROP SEQUENCE IF EXISTS public.advance_policies_advance_policy_id_seq;
DROP TABLE IF EXISTS public.advance_policies;
DROP TABLE IF EXISTS public.advance_payouts;
DROP FUNCTION IF EXISTS test.set_updated_at();
DROP EXTENSION IF EXISTS pgcrypto;
DROP EXTENSION IF EXISTS btree_gist;
DROP SCHEMA IF EXISTS test;
--
-- Name: test; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA test;


--
-- Name: btree_gist; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA test;


--
-- Name: EXTENSION btree_gist; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION btree_gist IS 'support for indexing common datatypes in GiST';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA test;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: set_updated_at(); Type: FUNCTION; Schema: test; Owner: -
--

CREATE FUNCTION test.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: advance_payouts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.advance_payouts (
    advance_payout_id character varying(64) NOT NULL,
    advance_request_id bigint NOT NULL,
    amount_atomic bigint NOT NULL,
    asset_symbol character varying(32) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    failure_reason character varying(500),
    idempotency_key character varying(128) NOT NULL,
    signed_transaction text,
    status character varying(20) NOT NULL,
    tx_hash character varying(66),
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    wallet_address character varying(42) NOT NULL,
    CONSTRAINT advance_payouts_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'SIGNED'::character varying, 'BROADCASTED'::character varying, 'CONFIRMED'::character varying, 'FAILED'::character varying, 'TIMED_OUT'::character varying])::text[])))
);


--
-- Name: advance_policies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.advance_policies (
    advance_policy_id bigint NOT NULL,
    asset_decimals integer NOT NULL,
    asset_symbol character varying(20) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    disclaimer text NOT NULL,
    enabled boolean NOT NULL,
    fee_type character varying(20) NOT NULL,
    flat_fee_display_krw_amount bigint NOT NULL,
    manual_repayment_enabled boolean NOT NULL,
    max_cap_display_krw_amount bigint NOT NULL,
    near_payday_max_cap_display_krw_amount bigint NOT NULL,
    payday_day integer NOT NULL,
    reduced_cap_days_before_payday integer NOT NULL,
    reference_krw_per_asset numeric(12,2) NOT NULL,
    same_day_advance_allowed boolean NOT NULL,
    settlement_mode character varying(40) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT advance_policies_fee_type_check CHECK (((fee_type)::text = 'FLAT'::text)),
    CONSTRAINT advance_policies_settlement_mode_check CHECK (((settlement_mode)::text = 'PAYDAY_AUTO_OFFSET'::text))
);


--
-- Name: advance_policies_advance_policy_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.advance_policies_advance_policy_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: advance_policies_advance_policy_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.advance_policies_advance_policy_id_seq OWNED BY public.advance_policies.advance_policy_id;


--
-- Name: advance_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.advance_requests (
    repayment_due_date date NOT NULL,
    snapshot_needs_review_record_count integer NOT NULL,
    snapshot_policy_rate numeric(5,2) NOT NULL,
    snapshot_reflected_work_days integer NOT NULL,
    year_month character varying(7) NOT NULL,
    approved_amount bigint,
    contract_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    fee_amount bigint,
    id bigint NOT NULL,
    requested_amount bigint,
    requested_at timestamp(6) without time zone NOT NULL,
    snapshot_available_amount bigint,
    snapshot_max_cap bigint,
    snapshot_reflected_work_minutes bigint NOT NULL,
    user_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    idempotency_key character varying(120) NOT NULL,
    reviewed_at timestamp(6) without time zone,
    reviewed_by_account_id bigint,
    approved_amount_atomic bigint,
    approved_reference_krw bigint,
    asset_decimals integer NOT NULL,
    asset_symbol character varying(20) NOT NULL,
    reference_exchange_rate numeric(12,2) NOT NULL,
    fee_amount_atomic bigint NOT NULL,
    fee_reference_krw bigint NOT NULL,
    requested_amount_atomic bigint NOT NULL,
    requested_reference_krw bigint NOT NULL,
    snapshot_available_amount_atomic bigint NOT NULL,
    snapshot_available_reference_krw bigint NOT NULL,
    snapshot_max_cap_amount_atomic bigint NOT NULL,
    snapshot_max_cap_reference_krw bigint NOT NULL,
    CONSTRAINT advance_requests_status_check CHECK (((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'NEEDS_REVIEW'::character varying])::text[])))
);


--
-- Name: advance_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.advance_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: advance_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.advance_requests_id_seq OWNED BY public.advance_requests.id;


--
-- Name: claim_preparations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.claim_preparations (
    claim_kit_document_id bigint,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    wage_verification_id bigint NOT NULL,
    locale character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    tone character varying(20) NOT NULL,
    summary_text character varying(2000) NOT NULL,
    CONSTRAINT claim_preparations_status_check CHECK (((status)::text = 'READY'::text)),
    CONSTRAINT claim_preparations_tone_check CHECK (((tone)::text = ANY ((ARRAY['DEFAULT'::character varying, 'POLITE'::character varying, 'SHORT'::character varying])::text[])))
);


--
-- Name: claim_preparations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.claim_preparations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: claim_preparations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.claim_preparations_id_seq OWNED BY public.claim_preparations.id;


--
-- Name: companies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.companies (
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    status character varying(20) NOT NULL,
    company_code character varying(50) NOT NULL,
    name character varying(120) NOT NULL,
    default_workplace_id bigint,
    overtime_rounding_unit character varying(20),
    scheduled_clock_in_time time(6) without time zone,
    scheduled_clock_out_time time(6) without time zone,
    CONSTRAINT companies_overtime_rounding_unit_check CHECK (((overtime_rounding_unit)::text = ANY ((ARRAY['FIFTEEN_MINUTES'::character varying, 'THIRTY_MINUTES'::character varying, 'ONE_HOUR'::character varying])::text[]))),
    CONSTRAINT companies_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: companies_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.companies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: companies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.companies_id_seq OWNED BY public.companies.id;


--
-- Name: correction_decision_audits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.correction_decision_audits (
    actor_account_id bigint NOT NULL,
    correction_request_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    after_status character varying(20) NOT NULL,
    before_status character varying(20) NOT NULL,
    reject_reason_code character varying(100),
    decision_memo character varying(500),
    CONSTRAINT correction_decision_audits_after_status_check CHECK (((after_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT correction_decision_audits_before_status_check CHECK (((before_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: correction_decision_audits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.correction_decision_audits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: correction_decision_audits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.correction_decision_audits_id_seq OWNED BY public.correction_decision_audits.id;


--
-- Name: correction_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.correction_requests (
    attachment_count integer NOT NULL,
    work_date date NOT NULL,
    company_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    decision_at timestamp(6) without time zone,
    decision_by_account_id bigint,
    id bigint NOT NULL,
    original_clock_in_at timestamp(6) without time zone NOT NULL,
    original_clock_out_at timestamp(6) without time zone NOT NULL,
    requested_by_account_id bigint NOT NULL,
    requested_clock_in_at timestamp(6) without time zone NOT NULL,
    requested_clock_out_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    work_proof_id bigint NOT NULL,
    worker_account_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    reject_reason_code character varying(100),
    decision_memo character varying(500),
    reason character varying(500) NOT NULL,
    request_memo character varying(500),
    attachment_metadata_json text,
    reason_code character varying(50),
    review_reason_code character varying(50),
    CONSTRAINT correction_requests_reason_code_check CHECK (((reason_code)::text = ANY ((ARRAY['LATE_BUTTON_PRESS'::character varying, 'LATE_CLOCK_IN'::character varying, 'EARLY_CLOCK_OUT'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT correction_requests_review_reason_code_check CHECK (((review_reason_code)::text = ANY ((ARRAY['LATE_CLOCK_IN_AFTER_SCHEDULE'::character varying, 'EARLY_CLOCK_OUT_BEFORE_SCHEDULE'::character varying, 'LATE_CLOCK_OUT_AFTER_GRACE'::character varying, 'OUTSIDE_ALLOWED_RADIUS'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT correction_requests_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: correction_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.correction_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: correction_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.correction_requests_id_seq OWNED BY public.correction_requests.id;


--
-- Name: document_generation_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.document_generation_requests (
    end_date date,
    include_attachments boolean NOT NULL,
    start_date date,
    year_month character varying(7),
    created_at timestamp(6) without time zone NOT NULL,
    generated_at timestamp(6) without time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    wage_verification_id bigint,
    workplace_id bigint NOT NULL,
    output_format character varying(10) NOT NULL,
    status character varying(20) NOT NULL,
    document_type character varying(30) NOT NULL,
    request_id character varying(36) NOT NULL,
    failure_reason character varying(100),
    idempotency_key character varying(120) NOT NULL,
    file_name character varying(255),
    CONSTRAINT document_generation_requests_document_type_check CHECK (((document_type)::text = ANY ((ARRAY['WORKPROOF_STATEMENT'::character varying, 'PROOF_PACK'::character varying, 'CLAIM_KIT'::character varying, 'TRANSFER_RECEIPT'::character varying])::text[]))),
    CONSTRAINT document_generation_requests_output_format_check CHECK (((output_format)::text = ANY ((ARRAY['PDF'::character varying, 'ZIP'::character varying])::text[]))),
    CONSTRAINT document_generation_requests_status_check CHECK (((status)::text = ANY ((ARRAY['QUEUED'::character varying, 'RUNNING'::character varying, 'READY'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: document_generation_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.document_generation_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: document_generation_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.document_generation_requests_id_seq OWNED BY public.document_generation_requests.id;


--
-- Name: employer_invitation_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employer_invitation_tokens (
    company_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    default_workplace_id bigint NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    invited_by_account_id bigint,
    revoked_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    used_at timestamp(6) without time zone,
    role character varying(20) NOT NULL,
    token_hash character varying(64) NOT NULL,
    invitee_email character varying(255) NOT NULL,
    CONSTRAINT employer_invitation_tokens_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'EMPLOYER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: employer_invitation_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employer_invitation_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employer_invitation_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employer_invitation_tokens_id_seq OWNED BY public.employer_invitation_tokens.id;


--
-- Name: employer_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employer_profiles (
    account_id bigint NOT NULL,
    company_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    default_workplace_id bigint NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    status character varying(20) NOT NULL,
    display_name character varying(100) NOT NULL,
    CONSTRAINT employer_profiles_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: employer_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employer_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employer_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employer_profiles_id_seq OWNED BY public.employer_profiles.id;


--
-- Name: employer_signup_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employer_signup_codes (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    code_hash character varying(64) NOT NULL,
    company_id bigint NOT NULL,
    default_workplace_id bigint NOT NULL,
    encrypted_code character varying(1024),
    issued_by_account_id bigint,
    revoked_at timestamp(6) without time zone
);


--
-- Name: employer_signup_codes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employer_signup_codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employer_signup_codes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employer_signup_codes_id_seq OWNED BY public.employer_signup_codes.id;


--
-- Name: employment_memberships; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employment_memberships (
    effective_from date NOT NULL,
    effective_to date,
    company_id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    worker_account_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT employment_memberships_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: employment_memberships_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employment_memberships_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employment_memberships_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employment_memberships_id_seq OWNED BY public.employment_memberships.id;


--
-- Name: jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobs (
    attempt_count integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    run_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    status character varying(20) NOT NULL,
    job_type character varying(40) NOT NULL,
    reference_kind character varying(40) NOT NULL,
    reference_id character varying(64) NOT NULL,
    active_key character varying(200),
    last_error character varying(500),
    CONSTRAINT jobs_job_type_check CHECK (((job_type)::text = ANY ((ARRAY['SUBMIT_TRANSFER'::character varying, 'POLL_TRANSFER_RECEIPT'::character varying, 'SUBMIT_VAULT_TRANSACTION'::character varying, 'POLL_VAULT_TRANSACTION_RECEIPT'::character varying, 'SUBMIT_ADVANCE_PAYOUT'::character varying, 'POLL_ADVANCE_PAYOUT_RECEIPT'::character varying])::text[]))),
    CONSTRAINT jobs_reference_kind_check CHECK (((reference_kind)::text = ANY ((ARRAY['TRANSFER'::character varying, 'VAULT'::character varying, 'ADVANCE_PAYOUT'::character varying])::text[]))),
    CONSTRAINT jobs_status_check CHECK (((status)::text = ANY ((ARRAY['QUEUED'::character varying, 'RUNNING'::character varying, 'DONE'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: jobs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.jobs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jobs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.jobs_id_seq OWNED BY public.jobs.id;


--
-- Name: recipients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recipients (
    allowed boolean NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    target_user_id bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    relation character varying(20) NOT NULL,
    wallet_address character varying(42) NOT NULL,
    recipient_id character varying(64) NOT NULL,
    alias character varying(100) NOT NULL,
    CONSTRAINT recipients_relation_check CHECK (((relation)::text = ANY ((ARRAY['FAMILY'::character varying, 'SPOUSE'::character varying, 'PARENT'::character varying, 'CHILD'::character varying, 'SIBLING'::character varying, 'RELATIVE'::character varying, 'FRIEND'::character varying, 'OTHER'::character varying])::text[])))
);


--
-- Name: transfers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transfers (
    high_amount_confirmed boolean NOT NULL,
    recent_recipient_confirmed boolean NOT NULL,
    amount_atomic bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    recipient_target_user_id_snapshot bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    asset_symbol character varying(20) NOT NULL,
    recipient_relation_snapshot character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    failure_code character varying(40),
    recipient_address character varying(42) NOT NULL,
    sender_address character varying(42) NOT NULL,
    recipient_id character varying(64) NOT NULL,
    transfer_id character varying(64) NOT NULL,
    tx_hash character varying(66),
    recipient_alias_snapshot character varying(100) NOT NULL,
    idempotency_key character varying(128) NOT NULL,
    signed_transaction text,
    network_fee_wei character varying(80),
    CONSTRAINT transfers_failure_code_check CHECK (((failure_code)::text = ANY ((ARRAY['NETWORK_ERROR'::character varying, 'CHAIN_REVERT'::character varying, 'INSUFFICIENT_GAS'::character varying, 'UNKNOWN'::character varying])::text[]))),
    CONSTRAINT transfers_recipient_relation_snapshot_check CHECK (((recipient_relation_snapshot)::text = ANY ((ARRAY['FAMILY'::character varying, 'SPOUSE'::character varying, 'PARENT'::character varying, 'CHILD'::character varying, 'SIBLING'::character varying, 'RELATIVE'::character varying, 'FRIEND'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT transfers_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'SIGNED'::character varying, 'BROADCASTED'::character varying, 'CONFIRMED'::character varying, 'FAILED'::character varying, 'TIMED_OUT'::character varying])::text[])))
);


--
-- Name: user_wallets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_wallets (
    created_at timestamp(6) without time zone NOT NULL,
    funded_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    funding_status character varying(20) NOT NULL,
    wallet_address character varying(42) NOT NULL,
    funding_failure_reason character varying(300),
    encrypted_private_key character varying(1024) NOT NULL,
    CONSTRAINT user_wallets_funding_status_check CHECK (((funding_status)::text = ANY ((ARRAY['PENDING'::character varying, 'FUNDED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    company_code character varying(12),
    phone_number character varying(20),
    role character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'EMPLOYER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vault_positions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vault_positions (
    id bigint NOT NULL,
    accrued_yield_atomic numeric(38,0) NOT NULL,
    apy_bps integer NOT NULL,
    asset_address character varying(128) NOT NULL,
    asset_symbol character varying(32) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    last_accrued_at timestamp(6) without time zone,
    network character varying(32) NOT NULL,
    principal_amount_atomic numeric(38,0) NOT NULL,
    share_balance numeric(38,0) NOT NULL,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    vault_address character varying(128) NOT NULL,
    wallet_address character varying(42) NOT NULL,
    CONSTRAINT vault_positions_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PAUSED'::character varying, 'CLOSED'::character varying])::text[])))
);


--
-- Name: vault_positions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vault_positions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vault_positions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vault_positions_id_seq OWNED BY public.vault_positions.id;


--
-- Name: vault_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vault_transactions (
    vault_transaction_id character varying(64) NOT NULL,
    amount_atomic numeric(38,0) NOT NULL,
    asset_symbol character varying(32) NOT NULL,
    confirmed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    failure_code character varying(40),
    idempotency_key character varying(128) NOT NULL,
    position_id bigint NOT NULL,
    share_delta numeric(38,0),
    signed_transaction text,
    status character varying(20) NOT NULL,
    tx_hash character varying(66),
    tx_type character varying(20) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    vault_address character varying(128) NOT NULL,
    wallet_address character varying(42) NOT NULL,
    CONSTRAINT vault_transactions_failure_code_check CHECK (((failure_code)::text = ANY ((ARRAY['INSUFFICIENT_BALANCE'::character varying, 'ALLOWANCE_FAILED'::character varying, 'NETWORK_ERROR'::character varying, 'UNKNOWN'::character varying])::text[]))),
    CONSTRAINT vault_transactions_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'SIGNED'::character varying, 'BROADCASTED'::character varying, 'CONFIRMED'::character varying, 'FAILED'::character varying, 'TIMED_OUT'::character varying])::text[]))),
    CONSTRAINT vault_transactions_tx_type_check CHECK (((tx_type)::text = ANY ((ARRAY['DEPOSIT'::character varying, 'WITHDRAW'::character varying])::text[])))
);


--
-- Name: vault_yield_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vault_yield_logs (
    id bigint NOT NULL,
    apy_bps integer NOT NULL,
    calculation_basis character varying(32) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    from_at timestamp(6) without time zone NOT NULL,
    position_id bigint NOT NULL,
    principal_amount_atomic numeric(38,0) NOT NULL,
    to_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    yield_amount_atomic numeric(38,0) NOT NULL
);


--
-- Name: vault_yield_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vault_yield_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vault_yield_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vault_yield_logs_id_seq OWNED BY public.vault_yield_logs.id;


--
-- Name: wage_deposits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wage_deposits (
    deductions_known boolean NOT NULL,
    deposit_date date NOT NULL,
    year_month character varying(7) NOT NULL,
    actual_deposit_amount bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    note character varying(500)
);


--
-- Name: wage_deposits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.wage_deposits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: wage_deposits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.wage_deposits_id_seq OWNED BY public.wage_deposits.id;


--
-- Name: wage_verification_possible_causes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wage_verification_possible_causes (
    cause_order integer NOT NULL,
    verification_id bigint NOT NULL,
    cause_code character varying(100) NOT NULL,
    cause_title character varying(200) NOT NULL,
    cause_detail character varying(500) NOT NULL
);


--
-- Name: wage_verification_record_ids; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wage_verification_record_ids (
    record_order integer NOT NULL,
    record_id bigint NOT NULL,
    verification_id bigint NOT NULL
);


--
-- Name: wage_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wage_verifications (
    deductions_known boolean NOT NULL,
    difference_rate numeric(10,4) NOT NULL,
    modified_record_count integer NOT NULL,
    threshold_deduction_relaxed boolean NOT NULL,
    threshold_relative_percent numeric(10,4) NOT NULL,
    year_month character varying(7) NOT NULL,
    actual_deposit_amount bigint NOT NULL,
    base_estimate bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    difference_amount bigint NOT NULL,
    estimated_total bigint NOT NULL,
    id bigint NOT NULL,
    night_minutes bigint NOT NULL,
    night_premium bigint NOT NULL,
    overtime_minutes bigint NOT NULL,
    overtime_premium bigint NOT NULL,
    threshold_absolute_won bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    status character varying(30) NOT NULL,
    resolution_stage character varying(50) NOT NULL,
    memo character varying(500),
    CONSTRAINT wage_verifications_resolution_stage_check CHECK (((resolution_stage)::text = ANY ((ARRAY['SELF_CHECK'::character varying, 'EMPLOYER_CONFIRMATION_RECOMMENDED'::character varying, 'EXTERNAL_HELP_PREPARATION'::character varying])::text[]))),
    CONSTRAINT wage_verifications_status_check CHECK (((status)::text = ANY ((ARRAY['MATCHED'::character varying, 'CHECK_REQUIRED'::character varying])::text[])))
);


--
-- Name: wage_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.wage_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: wage_verifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.wage_verifications_id_seq OWNED BY public.wage_verifications.id;


--
-- Name: work_contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_contracts (
    base_pay_amount numeric(19,2) NOT NULL,
    daily_work_minutes integer,
    effective_from date NOT NULL,
    effective_to date,
    monthly_work_minutes integer,
    normalized_hourly_wage numeric(19,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    workplace_id bigint NOT NULL,
    pay_unit character varying(20) NOT NULL,
    CONSTRAINT work_contracts_pay_unit_check CHECK (((pay_unit)::text = ANY ((ARRAY['HOURLY'::character varying, 'DAILY'::character varying, 'MONTHLY'::character varying])::text[])))
);


--
-- Name: work_contracts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.work_contracts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: work_contracts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.work_contracts_id_seq OWNED BY public.work_contracts.id;


--
-- Name: work_proof_audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_proof_audit_logs (
    after_attachment_count integer NOT NULL,
    before_attachment_count integer NOT NULL,
    actor_user_id bigint NOT NULL,
    after_clock_in_at timestamp(6) without time zone NOT NULL,
    after_clock_out_at timestamp(6) without time zone NOT NULL,
    before_clock_in_at timestamp(6) without time zone NOT NULL,
    before_clock_out_at timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    work_proof_id bigint NOT NULL,
    after_edit_reason character varying(500) NOT NULL,
    after_memo character varying(500),
    before_edit_reason character varying(500),
    before_memo character varying(500),
    after_attachment_metadata_json text,
    before_attachment_metadata_json text
);


--
-- Name: work_proof_audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.work_proof_audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: work_proof_audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.work_proof_audit_logs_id_seq OWNED BY public.work_proof_audit_logs.id;


--
-- Name: work_proofs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.work_proofs (
    attachment_count integer NOT NULL,
    clock_in_latitude double precision NOT NULL,
    clock_in_longitude double precision NOT NULL,
    clock_out_latitude double precision,
    clock_out_longitude double precision,
    clock_out_outside_allowed_radius boolean,
    work_date date NOT NULL,
    workplace_latitude_snapshot double precision,
    workplace_longitude_snapshot double precision,
    clock_in_at timestamp(6) without time zone NOT NULL,
    clock_out_at timestamp(6) without time zone,
    contract_id bigint,
    created_at timestamp(6) without time zone NOT NULL,
    device_clock_in_at timestamp(6) without time zone NOT NULL,
    device_clock_out_at timestamp(6) without time zone,
    id bigint NOT NULL,
    server_clock_in_at timestamp(6) without time zone NOT NULL,
    server_clock_out_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    workplace_id bigint,
    financial_status character varying(20) NOT NULL,
    clock_in_location_label character varying(100),
    clock_out_location_label character varying(100),
    workplace_map_label_snapshot character varying(100),
    workplace_name_snapshot character varying(100),
    edit_reason character varying(500),
    memo character varying(500),
    workplace_address_snapshot character varying(255),
    attachment_metadata_json text,
    recognized_clock_in_at timestamp(6) without time zone,
    recognized_clock_out_at timestamp(6) without time zone,
    CONSTRAINT work_proofs_financial_status_check CHECK (((financial_status)::text = ANY ((ARRAY['PENDING'::character varying, 'NEEDS_REVIEW'::character varying, 'REFLECTED'::character varying])::text[])))
);


--
-- Name: work_proofs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.work_proofs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: work_proofs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.work_proofs_id_seq OWNED BY public.work_proofs.id;


--
-- Name: worker_registration_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.worker_registration_codes (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    code_hash character varying(64) NOT NULL,
    company_id bigint NOT NULL,
    encrypted_code character varying(1024) NOT NULL,
    issued_by_account_id bigint NOT NULL,
    revoked_at timestamp(6) without time zone,
    workplace_id bigint NOT NULL
);


--
-- Name: worker_registration_codes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.worker_registration_codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: worker_registration_codes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.worker_registration_codes_id_seq OWNED BY public.worker_registration_codes.id;


--
-- Name: workplaces; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workplaces (
    allowed_radius_meters integer,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    company_id bigint,
    created_at timestamp(6) without time zone NOT NULL,
    id bigint NOT NULL,
    settings_effective_from timestamp(6) without time zone,
    settings_updated_by_account_id bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id bigint NOT NULL,
    map_label character varying(100),
    name character varying(100) NOT NULL,
    address character varying(255) NOT NULL
);


--
-- Name: workplaces_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workplaces_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workplaces_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workplaces_id_seq OWNED BY public.workplaces.id;


--
-- Name: advance_requests; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.advance_requests (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    month_start date NOT NULL,
    idempotency_key character varying(120) NOT NULL,
    requested_amount_krw bigint NOT NULL,
    approved_amount_krw bigint NOT NULL,
    fee_amount_krw bigint NOT NULL,
    status character varying(20) NOT NULL,
    requested_at timestamp with time zone NOT NULL,
    repayment_due_date date NOT NULL,
    snapshot_available_amount_krw bigint NOT NULL,
    snapshot_max_cap_krw bigint NOT NULL,
    snapshot_policy_rate numeric(10,4) NOT NULL,
    snapshot_repayment_tier character varying(2) NOT NULL,
    snapshot_reflected_work_days integer NOT NULL,
    snapshot_reflected_work_minutes bigint NOT NULL,
    snapshot_verified_minutes bigint NOT NULL,
    snapshot_pending_minutes bigint NOT NULL,
    snapshot_needs_review_record_count integer NOT NULL,
    snapshot_next_tier_remaining_minutes integer NOT NULL,
    snapshot_estimated_fee_krw bigint NOT NULL,
    snapshot_estimated_repayment_date date NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_advance_requests_amounts CHECK (((requested_amount_krw > 0) AND (approved_amount_krw >= 0) AND (approved_amount_krw <= requested_amount_krw) AND (fee_amount_krw >= 0) AND (snapshot_available_amount_krw >= 0) AND (snapshot_max_cap_krw >= 0) AND (snapshot_estimated_fee_krw >= 0))),
    CONSTRAINT ck_advance_requests_due_date CHECK (((repayment_due_date >= (requested_at)::date) AND (snapshot_estimated_repayment_date >= (requested_at)::date))),
    CONSTRAINT ck_advance_requests_month_start CHECK ((month_start = (date_trunc('month'::text, (month_start)::timestamp with time zone))::date)),
    CONSTRAINT ck_advance_requests_snapshot_counts CHECK (((snapshot_reflected_work_days >= 0) AND (snapshot_reflected_work_minutes >= 0) AND (snapshot_verified_minutes >= 0) AND (snapshot_pending_minutes >= 0) AND (snapshot_needs_review_record_count >= 0) AND (snapshot_next_tier_remaining_minutes >= 0))),
    CONSTRAINT ck_advance_requests_snapshot_policy CHECK (((snapshot_policy_rate >= (0)::numeric) AND (snapshot_policy_rate <= (1)::numeric))),
    CONSTRAINT ck_advance_requests_snapshot_tier CHECK (((snapshot_repayment_tier)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying, 'C'::character varying, 'D'::character varying])::text[]))),
    CONSTRAINT ck_advance_requests_status CHECK (((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'NEEDS_REVIEW'::character varying])::text[])))
);


--
-- Name: advance_requests_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.advance_requests ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.advance_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: async_job_attempts; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.async_job_attempts (
    id bigint NOT NULL,
    job_id bigint NOT NULL,
    attempt_no integer NOT NULL,
    status character varying(20) NOT NULL,
    worker_name character varying(120),
    started_at timestamp with time zone NOT NULL,
    finished_at timestamp with time zone,
    error_code character varying(50),
    error_message text,
    CONSTRAINT ck_async_job_attempts_attempt_no CHECK ((attempt_no > 0)),
    CONSTRAINT ck_async_job_attempts_finished_at CHECK (((((status)::text = ANY ((ARRAY['DONE'::character varying, 'FAILED'::character varying])::text[])) AND (finished_at IS NOT NULL)) OR ((status)::text = 'RUNNING'::text))),
    CONSTRAINT ck_async_job_attempts_status CHECK (((status)::text = ANY ((ARRAY['RUNNING'::character varying, 'DONE'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: async_job_attempts_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.async_job_attempts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.async_job_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: async_jobs; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.async_jobs (
    id bigint NOT NULL,
    job_type character varying(40) NOT NULL,
    queue_name character varying(50) DEFAULT 'default'::character varying NOT NULL,
    resource_type character varying(50) NOT NULL,
    resource_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    priority integer DEFAULT 100 NOT NULL,
    dedupe_key character varying(200),
    concurrency_key character varying(200),
    payload_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    max_attempts integer DEFAULT 5 NOT NULL,
    run_after timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    locked_at timestamp with time zone,
    locked_by character varying(120),
    last_error_code character varying(50),
    last_error_message text,
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_async_jobs_attempts CHECK (((attempts >= 0) AND (max_attempts > 0) AND (attempts <= max_attempts))),
    CONSTRAINT ck_async_jobs_completed_at CHECK (((((status)::text = ANY ((ARRAY['DONE'::character varying, 'FAILED'::character varying])::text[])) AND (completed_at IS NOT NULL)) OR ((status)::text = ANY ((ARRAY['QUEUED'::character varying, 'RUNNING'::character varying])::text[])))),
    CONSTRAINT ck_async_jobs_priority CHECK ((priority >= 0)),
    CONSTRAINT ck_async_jobs_status CHECK (((status)::text = ANY ((ARRAY['QUEUED'::character varying, 'RUNNING'::character varying, 'DONE'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT ck_async_jobs_type CHECK (((job_type)::text = ANY ((ARRAY['RENDER_PROOF_PACK'::character varying, 'RENDER_CLAIM_KIT'::character varying, 'SUBMIT_TRANSFER'::character varying, 'POLL_TRANSFER_STATUS'::character varying, 'RENDER_TRANSFER_RECEIPT'::character varying])::text[])))
);


--
-- Name: async_jobs_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.async_jobs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.async_jobs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: claim_preparation_checklist_items; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.claim_preparation_checklist_items (
    id bigint NOT NULL,
    preparation_id bigint NOT NULL,
    sort_order integer NOT NULL,
    item_code character varying(50),
    item_text text NOT NULL,
    is_required boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_claim_preparation_checklist_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: claim_preparation_checklist_items_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.claim_preparation_checklist_items ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.claim_preparation_checklist_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: claim_preparation_document_links; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.claim_preparation_document_links (
    preparation_id bigint NOT NULL,
    document_id bigint NOT NULL,
    sort_order integer NOT NULL,
    CONSTRAINT ck_claim_preparation_document_links_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: claim_preparation_route_snapshots; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.claim_preparation_route_snapshots (
    id bigint NOT NULL,
    preparation_id bigint NOT NULL,
    sort_order integer NOT NULL,
    channel character varying(20) NOT NULL,
    title character varying(150) NOT NULL,
    description text NOT NULL,
    contact character varying(200),
    link_url character varying(500),
    CONSTRAINT ck_claim_preparation_routes_channel CHECK (((channel)::text = ANY ((ARRAY['ONLINE'::character varying, 'PHONE'::character varying, 'VISIT'::character varying])::text[]))),
    CONSTRAINT ck_claim_preparation_routes_contact_or_link CHECK (((contact IS NOT NULL) OR (link_url IS NOT NULL))),
    CONSTRAINT ck_claim_preparation_routes_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: claim_preparation_route_snapshots_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.claim_preparation_route_snapshots ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.claim_preparation_route_snapshots_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: claim_preparations; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.claim_preparations (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    wage_verification_id bigint NOT NULL,
    claim_kit_document_id bigint,
    idempotency_key character varying(120),
    locale character varying(20) NOT NULL,
    tone character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'READY'::character varying NOT NULL,
    summary_text text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_claim_preparations_status CHECK (((status)::text = ANY ((ARRAY['READY'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT ck_claim_preparations_summary_when_ready CHECK ((((status)::text <> 'READY'::text) OR (summary_text IS NOT NULL))),
    CONSTRAINT ck_claim_preparations_tone CHECK (((tone)::text = ANY ((ARRAY['DEFAULT'::character varying, 'POLITE'::character varying, 'SHORT'::character varying])::text[])))
);


--
-- Name: claim_preparations_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.claim_preparations ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.claim_preparations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: claim_routes; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.claim_routes (
    id bigint NOT NULL,
    locale character varying(20) NOT NULL,
    channel character varying(20) NOT NULL,
    title character varying(150) NOT NULL,
    description text NOT NULL,
    contact character varying(200),
    link_url character varying(500),
    sort_order integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_claim_routes_channel CHECK (((channel)::text = ANY ((ARRAY['ONLINE'::character varying, 'PHONE'::character varying, 'VISIT'::character varying])::text[]))),
    CONSTRAINT ck_claim_routes_contact_or_link CHECK (((contact IS NOT NULL) OR (link_url IS NOT NULL))),
    CONSTRAINT ck_claim_routes_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: claim_routes_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.claim_routes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.claim_routes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: documents; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.documents (
    id bigint NOT NULL,
    request_id uuid DEFAULT gen_random_uuid() NOT NULL,
    requested_by_user_id bigint NOT NULL,
    document_type character varying(30) NOT NULL,
    document_status character varying(20) NOT NULL,
    document_format character varying(10) NOT NULL,
    title character varying(200) NOT NULL,
    idempotency_key character varying(120) NOT NULL,
    wage_verification_id bigint,
    transfer_id bigint,
    workplace_id bigint,
    month_start date,
    summary_payload_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    related_links_json jsonb DEFAULT '[]'::jsonb NOT NULL,
    mime_type character varying(120),
    file_name character varying(255),
    file_size_bytes bigint,
    storage_provider character varying(40),
    storage_object_key character varying(512),
    file_sha256 character(64),
    error_code character varying(50),
    error_message text,
    queued_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    started_at timestamp with time zone,
    ready_at timestamp with time zone,
    failed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_documents_anchor_by_type CHECK (((((document_type)::text = ANY ((ARRAY['PROOF_PACK'::character varying, 'CLAIM_KIT'::character varying])::text[])) AND (wage_verification_id IS NOT NULL) AND (transfer_id IS NULL) AND (workplace_id IS NOT NULL) AND (month_start IS NOT NULL)) OR (((document_type)::text = 'TRANSFER_RECEIPT'::text) AND (transfer_id IS NOT NULL) AND (wage_verification_id IS NULL) AND (workplace_id IS NULL) AND (month_start IS NULL)))),
    CONSTRAINT ck_documents_file_size CHECK (((file_size_bytes IS NULL) OR (file_size_bytes > 0))),
    CONSTRAINT ck_documents_format CHECK (((document_format)::text = ANY ((ARRAY['PDF'::character varying, 'ZIP'::character varying])::text[]))),
    CONSTRAINT ck_documents_format_by_type CHECK (((((document_type)::text = 'CLAIM_KIT'::text) AND ((document_format)::text = ANY ((ARRAY['PDF'::character varying, 'ZIP'::character varying])::text[]))) OR (((document_type)::text = ANY ((ARRAY['PROOF_PACK'::character varying, 'TRANSFER_RECEIPT'::character varying])::text[])) AND ((document_format)::text = 'PDF'::text)))),
    CONSTRAINT ck_documents_month_start CHECK (((month_start IS NULL) OR (month_start = (date_trunc('month'::text, (month_start)::timestamp with time zone))::date))),
    CONSTRAINT ck_documents_ready_file_payload CHECK ((((document_status)::text <> 'READY'::text) OR ((mime_type IS NOT NULL) AND (file_name IS NOT NULL) AND (file_size_bytes IS NOT NULL) AND (storage_provider IS NOT NULL) AND (storage_object_key IS NOT NULL)))),
    CONSTRAINT ck_documents_sha256 CHECK (((file_sha256 IS NULL) OR (file_sha256 ~ '^[0-9a-f]{64}$'::text))),
    CONSTRAINT ck_documents_status CHECK (((document_status)::text = ANY ((ARRAY['QUEUED'::character varying, 'RUNNING'::character varying, 'READY'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT ck_documents_status_timestamps CHECK (((((document_status)::text <> 'RUNNING'::text) OR (started_at IS NOT NULL)) AND (((document_status)::text <> 'READY'::text) OR ((started_at IS NOT NULL) AND (ready_at IS NOT NULL))) AND (((document_status)::text <> 'FAILED'::text) OR (failed_at IS NOT NULL)))),
    CONSTRAINT ck_documents_type CHECK (((document_type)::text = ANY ((ARRAY['PROOF_PACK'::character varying, 'CLAIM_KIT'::character varying, 'TRANSFER_RECEIPT'::character varying])::text[])))
);


--
-- Name: documents_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.documents ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: remittance_recipients; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.remittance_recipients (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    alias character varying(100) NOT NULL,
    relationship character varying(50) NOT NULL,
    wallet_address character varying(120) NOT NULL,
    photo_url character varying(500),
    is_favorite boolean DEFAULT false NOT NULL,
    cooldown_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: remittance_recipients_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.remittance_recipients ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.remittance_recipients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: remittance_transfer_events; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.remittance_transfer_events (
    id bigint NOT NULL,
    transfer_id bigint NOT NULL,
    event_type character varying(30) NOT NULL,
    message text,
    tx_hash character varying(120),
    payload_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_remittance_transfer_events_type CHECK (((event_type)::text = ANY ((ARRAY['REQUESTED'::character varying, 'SAFEPAY_CHECKED'::character varying, 'BLOCKED'::character varying, 'SUBMITTED_TO_CHAIN'::character varying, 'CONFIRMED'::character varying, 'FAILED'::character varying, 'RECEIPT_REQUESTED'::character varying, 'RECEIPT_READY'::character varying])::text[])))
);


--
-- Name: remittance_transfer_events_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.remittance_transfer_events ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.remittance_transfer_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: remittance_transfers; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.remittance_transfers (
    id bigint NOT NULL,
    request_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id bigint NOT NULL,
    recipient_id bigint NOT NULL,
    safepay_check_id bigint NOT NULL,
    idempotency_key character varying(120) NOT NULL,
    token_symbol character varying(20) NOT NULL,
    token_decimals integer DEFAULT 6 NOT NULL,
    amount numeric(36,18) NOT NULL,
    memo character varying(500),
    status character varying(20) NOT NULL,
    tx_hash character varying(120),
    failure_reason_code character varying(50),
    failure_reason_message text,
    submitted_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    confirmed_at timestamp with time zone,
    failed_at timestamp with time zone,
    blocked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_remittance_transfers_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT ck_remittance_transfers_status CHECK (((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'CONFIRMED'::character varying, 'FAILED'::character varying, 'BLOCKED'::character varying])::text[]))),
    CONSTRAINT ck_remittance_transfers_status_timestamps CHECK (((((status)::text <> 'CONFIRMED'::text) OR (confirmed_at IS NOT NULL)) AND (((status)::text <> 'FAILED'::text) OR (failed_at IS NOT NULL)) AND (((status)::text <> 'BLOCKED'::text) OR (blocked_at IS NOT NULL)))),
    CONSTRAINT ck_remittance_transfers_token_decimals CHECK (((token_decimals >= 0) AND (token_decimals <= 38)))
);


--
-- Name: remittance_transfers_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.remittance_transfers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.remittance_transfers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: safepay_transfer_check_reasons; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.safepay_transfer_check_reasons (
    id bigint NOT NULL,
    safepay_check_id bigint NOT NULL,
    sort_order integer NOT NULL,
    reason_code character varying(50) NOT NULL,
    CONSTRAINT ck_safepay_transfer_check_reasons_code CHECK (((reason_code)::text = ANY ((ARRAY['RECIPIENT_IN_COOLDOWN'::character varying, 'AMOUNT_TOO_HIGH'::character varying, 'RECIPIENT_NOT_ALLOWLISTED'::character varying, 'DUPLICATE_TRANSFER_SUSPECTED'::character varying])::text[]))),
    CONSTRAINT ck_safepay_transfer_check_reasons_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: safepay_transfer_check_reasons_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.safepay_transfer_check_reasons ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.safepay_transfer_check_reasons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: safepay_transfer_checks; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.safepay_transfer_checks (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    recipient_id bigint NOT NULL,
    token_symbol character varying(20) NOT NULL,
    amount numeric(36,18) NOT NULL,
    decision character varying(10) NOT NULL,
    user_message text NOT NULL,
    cooldown_until timestamp with time zone,
    requires_additional_confirm boolean DEFAULT false NOT NULL,
    checked_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_safepay_transfer_checks_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT ck_safepay_transfer_checks_decision CHECK (((decision)::text = ANY ((ARRAY['ALLOW'::character varying, 'WARN'::character varying, 'BLOCK'::character varying])::text[])))
);


--
-- Name: safepay_transfer_checks_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.safepay_transfer_checks ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.safepay_transfer_checks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: users; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.users (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(100) NOT NULL,
    role character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_users_role CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vault_accounts; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.vault_accounts (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token_symbol character varying(20) NOT NULL,
    stored_amount numeric(36,18) DEFAULT 0 NOT NULL,
    available_to_store_amount numeric(36,18) DEFAULT 0 NOT NULL,
    available_to_transfer_amount numeric(36,18) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_vault_accounts_balances CHECK (((stored_amount >= (0)::numeric) AND (available_to_store_amount >= (0)::numeric) AND (available_to_transfer_amount >= (0)::numeric)))
);


--
-- Name: vault_accounts_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.vault_accounts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.vault_accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: vault_ledger_entries; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.vault_ledger_entries (
    id bigint NOT NULL,
    account_id bigint NOT NULL,
    entry_type character varying(20) NOT NULL,
    amount numeric(36,18) NOT NULL,
    release_target character varying(20),
    resulting_stored_amount numeric(36,18) NOT NULL,
    resulting_available_to_store_amount numeric(36,18) NOT NULL,
    resulting_available_to_transfer_amount numeric(36,18) NOT NULL,
    interest_daily numeric(36,18) DEFAULT 0 NOT NULL,
    interest_monthly numeric(36,18) DEFAULT 0 NOT NULL,
    interest_apr numeric(10,4) DEFAULT 0 NOT NULL,
    simulated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_vault_ledger_entries_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT ck_vault_ledger_entries_release_target CHECK (((((entry_type)::text = 'RELEASE'::text) AND ((release_target)::text = ANY ((ARRAY['SPENDABLE'::character varying, 'TRANSFERABLE'::character varying])::text[]))) OR (((entry_type)::text <> 'RELEASE'::text) AND (release_target IS NULL)))),
    CONSTRAINT ck_vault_ledger_entries_resulting_balances CHECK (((resulting_stored_amount >= (0)::numeric) AND (resulting_available_to_store_amount >= (0)::numeric) AND (resulting_available_to_transfer_amount >= (0)::numeric) AND (interest_daily >= (0)::numeric) AND (interest_monthly >= (0)::numeric) AND (interest_apr >= (0)::numeric))),
    CONSTRAINT ck_vault_ledger_entries_type CHECK (((entry_type)::text = ANY ((ARRAY['ALLOCATION'::character varying, 'RELEASE'::character varying, 'ADJUSTMENT'::character varying])::text[])))
);


--
-- Name: vault_ledger_entries_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.vault_ledger_entries ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.vault_ledger_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: wage_deposits; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.wage_deposits (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    month_start date NOT NULL,
    deposit_date date NOT NULL,
    actual_deposit_amount_krw bigint NOT NULL,
    deductions_known boolean DEFAULT false NOT NULL,
    note character varying(500),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_wage_deposits_amount CHECK ((actual_deposit_amount_krw >= 0)),
    CONSTRAINT ck_wage_deposits_month_start CHECK ((month_start = (date_trunc('month'::text, (month_start)::timestamp with time zone))::date))
);


--
-- Name: wage_deposits_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.wage_deposits ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.wage_deposits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: wage_verification_cause_snapshots; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.wage_verification_cause_snapshots (
    id bigint NOT NULL,
    verification_id bigint NOT NULL,
    sort_order integer NOT NULL,
    cause_code character varying(50) NOT NULL,
    cause_title character varying(150) NOT NULL,
    cause_detail text,
    CONSTRAINT ck_wage_verification_cause_snapshots_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: wage_verification_cause_snapshots_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.wage_verification_cause_snapshots ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.wage_verification_cause_snapshots_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: wage_verification_record_links; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.wage_verification_record_links (
    verification_id bigint NOT NULL,
    sort_order integer NOT NULL,
    work_proof_record_id bigint NOT NULL,
    CONSTRAINT ck_wage_verification_record_links_sort_order CHECK ((sort_order >= 0))
);


--
-- Name: wage_verifications; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.wage_verifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    month_start date NOT NULL,
    pay_unit character varying(20) NOT NULL,
    base_pay_amount_krw bigint NOT NULL,
    daily_work_minutes integer,
    monthly_work_minutes integer,
    normalized_hourly_wage_krw bigint NOT NULL,
    work_day_count integer NOT NULL,
    verified_work_minutes bigint NOT NULL,
    overtime_minutes bigint NOT NULL,
    night_minutes bigint NOT NULL,
    modified_record_count integer NOT NULL,
    excluded_pending_record_count integer NOT NULL,
    actual_deposit_amount_krw bigint NOT NULL,
    deductions_known boolean NOT NULL,
    submitted_by character varying(20) DEFAULT 'WORKER'::character varying NOT NULL,
    memo character varying(500),
    status character varying(30) NOT NULL,
    resolution_stage character varying(50) NOT NULL,
    base_estimate_krw bigint NOT NULL,
    overtime_premium_krw bigint NOT NULL,
    night_premium_krw bigint NOT NULL,
    estimated_total_krw bigint NOT NULL,
    difference_amount_krw bigint NOT NULL,
    difference_rate numeric(10,4) NOT NULL,
    threshold_absolute_won bigint NOT NULL,
    threshold_relative_percent numeric(10,4) NOT NULL,
    threshold_deduction_relaxed boolean NOT NULL,
    rule_version character varying(50) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_wage_verifications_contract_amounts CHECK (((base_pay_amount_krw > 0) AND (normalized_hourly_wage_krw > 0))),
    CONSTRAINT ck_wage_verifications_counts CHECK (((work_day_count >= 0) AND (verified_work_minutes >= 0) AND (overtime_minutes >= 0) AND (night_minutes >= 0) AND (modified_record_count >= 0) AND (excluded_pending_record_count >= 0))),
    CONSTRAINT ck_wage_verifications_month_start CHECK ((month_start = (date_trunc('month'::text, (month_start)::timestamp with time zone))::date)),
    CONSTRAINT ck_wage_verifications_optional_minutes CHECK ((((daily_work_minutes IS NULL) OR (daily_work_minutes > 0)) AND ((monthly_work_minutes IS NULL) OR (monthly_work_minutes > 0)))),
    CONSTRAINT ck_wage_verifications_pay_unit CHECK (((pay_unit)::text = ANY ((ARRAY['HOURLY'::character varying, 'DAILY'::character varying, 'MONTHLY'::character varying])::text[]))),
    CONSTRAINT ck_wage_verifications_resolution_stage CHECK (((resolution_stage)::text = ANY ((ARRAY['SELF_CHECK'::character varying, 'EMPLOYER_CONFIRMATION_RECOMMENDED'::character varying, 'EXTERNAL_HELP_PREPARATION'::character varying])::text[]))),
    CONSTRAINT ck_wage_verifications_status CHECK (((status)::text = ANY ((ARRAY['MATCHED'::character varying, 'CHECK_REQUIRED'::character varying])::text[]))),
    CONSTRAINT ck_wage_verifications_submitted_by CHECK (((submitted_by)::text = 'WORKER'::text)),
    CONSTRAINT ck_wage_verifications_totals CHECK (((actual_deposit_amount_krw >= 0) AND (base_estimate_krw >= 0) AND (overtime_premium_krw >= 0) AND (night_premium_krw >= 0) AND (estimated_total_krw = ((base_estimate_krw + overtime_premium_krw) + night_premium_krw)) AND (difference_amount_krw = abs((estimated_total_krw - actual_deposit_amount_krw))) AND (difference_rate >= (0)::numeric) AND (threshold_absolute_won >= 0) AND (threshold_relative_percent >= (0)::numeric))),
    CONSTRAINT ck_wage_verifications_unit_payload CHECK (((((pay_unit)::text = 'HOURLY'::text) AND (daily_work_minutes IS NULL) AND (monthly_work_minutes IS NULL)) OR (((pay_unit)::text = 'DAILY'::text) AND (daily_work_minutes IS NOT NULL) AND (monthly_work_minutes IS NULL)) OR (((pay_unit)::text = 'MONTHLY'::text) AND (monthly_work_minutes IS NOT NULL))))
);


--
-- Name: wage_verifications_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.wage_verifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.wage_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: work_contracts; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.work_contracts (
    id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    pay_unit character varying(20) NOT NULL,
    base_pay_amount_krw bigint NOT NULL,
    daily_work_minutes integer,
    monthly_work_minutes integer,
    normalized_hourly_wage_krw bigint NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_work_contracts_base_pay CHECK ((base_pay_amount_krw > 0)),
    CONSTRAINT ck_work_contracts_daily_minutes CHECK (((daily_work_minutes IS NULL) OR (daily_work_minutes > 0))),
    CONSTRAINT ck_work_contracts_date_range CHECK (((effective_to IS NULL) OR (effective_to >= effective_from))),
    CONSTRAINT ck_work_contracts_monthly_minutes CHECK (((monthly_work_minutes IS NULL) OR (monthly_work_minutes > 0))),
    CONSTRAINT ck_work_contracts_normalized_hourly CHECK ((normalized_hourly_wage_krw > 0)),
    CONSTRAINT ck_work_contracts_pay_unit CHECK (((pay_unit)::text = ANY ((ARRAY['HOURLY'::character varying, 'DAILY'::character varying, 'MONTHLY'::character varying])::text[]))),
    CONSTRAINT ck_work_contracts_unit_payload CHECK (((((pay_unit)::text = 'HOURLY'::text) AND (daily_work_minutes IS NULL) AND (monthly_work_minutes IS NULL)) OR (((pay_unit)::text = 'DAILY'::text) AND (daily_work_minutes IS NOT NULL) AND (monthly_work_minutes IS NULL)) OR (((pay_unit)::text = 'MONTHLY'::text) AND (monthly_work_minutes IS NOT NULL))))
);


--
-- Name: work_contracts_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.work_contracts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.work_contracts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: work_proof_attachments; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.work_proof_attachments (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    kind character varying(30) NOT NULL,
    file_name character varying(255) NOT NULL,
    content_type character varying(120) NOT NULL,
    file_size_bytes bigint NOT NULL,
    storage_provider character varying(40) NOT NULL,
    storage_object_key character varying(512) NOT NULL,
    file_sha256 character(64),
    uploaded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_work_proof_attachments_kind CHECK (((kind)::text = ANY ((ARRAY['PHOTO'::character varying, 'MEMO_IMAGE'::character varying, 'SCHEDULE_IMAGE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_attachments_sha256 CHECK (((file_sha256 IS NULL) OR (file_sha256 ~ '^[0-9a-f]{64}$'::text))),
    CONSTRAINT ck_work_proof_attachments_size CHECK ((file_size_bytes > 0))
);


--
-- Name: work_proof_attachments_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.work_proof_attachments ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.work_proof_attachments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: work_proof_modification_attachments; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.work_proof_modification_attachments (
    modification_id bigint NOT NULL,
    attachment_id bigint NOT NULL
);


--
-- Name: work_proof_modifications; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.work_proof_modifications (
    id bigint NOT NULL,
    record_id bigint NOT NULL,
    modified_by_user_id bigint NOT NULL,
    reviewed_by_user_id bigint,
    review_status character varying(20) NOT NULL,
    modification_kind character varying(30) NOT NULL,
    reason_code character varying(30) NOT NULL,
    reason_memo character varying(500),
    before_record_status character varying(20),
    before_reflection_status character varying(20),
    before_check_in_device_at timestamp with time zone,
    before_check_out_device_at timestamp with time zone,
    after_record_status character varying(20),
    after_reflection_status character varying(20),
    after_check_in_device_at timestamp with time zone,
    after_check_out_device_at timestamp with time zone,
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_work_proof_modifications_after_record_status CHECK (((after_record_status IS NULL) OR ((after_record_status)::text = ANY ((ARRAY['CHECKED_IN'::character varying, 'CHECKED_OUT'::character varying, 'NEEDS_REVIEW'::character varying])::text[])))),
    CONSTRAINT ck_work_proof_modifications_after_reflection_status CHECK (((after_reflection_status IS NULL) OR ((after_reflection_status)::text = ANY ((ARRAY['PENDING'::character varying, 'REFLECTED'::character varying, 'NEEDS_REVIEW'::character varying, 'EXCLUDED'::character varying])::text[])))),
    CONSTRAINT ck_work_proof_modifications_before_record_status CHECK (((before_record_status IS NULL) OR ((before_record_status)::text = ANY ((ARRAY['CHECKED_IN'::character varying, 'CHECKED_OUT'::character varying, 'NEEDS_REVIEW'::character varying])::text[])))),
    CONSTRAINT ck_work_proof_modifications_before_reflection_status CHECK (((before_reflection_status IS NULL) OR ((before_reflection_status)::text = ANY ((ARRAY['PENDING'::character varying, 'REFLECTED'::character varying, 'NEEDS_REVIEW'::character varying, 'EXCLUDED'::character varying])::text[])))),
    CONSTRAINT ck_work_proof_modifications_kind CHECK (((modification_kind)::text = ANY ((ARRAY['RECORD_CORRECTION'::character varying, 'MANUAL_MISSING_RECORD'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_modifications_kind_reason CHECK (((((modification_kind)::text = 'MANUAL_MISSING_RECORD'::text) AND ((reason_code)::text = 'MISSING_RECORD'::text)) OR (((modification_kind)::text = 'RECORD_CORRECTION'::text) AND ((reason_code)::text = ANY ((ARRAY['LATE_TAP'::character varying, 'OVERTIME'::character varying, 'BREAK_CHANGED'::character varying, 'OTHER'::character varying])::text[]))))),
    CONSTRAINT ck_work_proof_modifications_reason_code CHECK (((reason_code)::text = ANY ((ARRAY['MISSING_RECORD'::character varying, 'LATE_TAP'::character varying, 'OVERTIME'::character varying, 'BREAK_CHANGED'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_modifications_review_status CHECK (((review_status)::text = ANY ((ARRAY['PENDING_REVIEW'::character varying, 'APPLIED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_modifications_reviewed_pair CHECK ((((reviewed_by_user_id IS NULL) AND (reviewed_at IS NULL)) OR ((reviewed_by_user_id IS NOT NULL) AND (reviewed_at IS NOT NULL))))
);


--
-- Name: work_proof_modifications_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.work_proof_modifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.work_proof_modifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: work_proof_records; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.work_proof_records (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    workplace_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    source_type character varying(30) NOT NULL,
    record_status character varying(20) NOT NULL,
    reflection_status character varying(20) NOT NULL,
    work_date date NOT NULL,
    worked_minutes integer,
    modification_count integer DEFAULT 0 NOT NULL,
    check_in_device_at timestamp with time zone,
    check_in_server_at timestamp with time zone,
    check_in_latitude numeric(9,6),
    check_in_longitude numeric(9,6),
    check_in_location_label character varying(120),
    check_out_device_at timestamp with time zone,
    check_out_server_at timestamp with time zone,
    check_out_latitude numeric(9,6),
    check_out_longitude numeric(9,6),
    check_out_location_label character varying(120),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_work_proof_records_check_in_coords CHECK ((((check_in_latitude IS NULL) AND (check_in_longitude IS NULL)) OR ((check_in_latitude IS NOT NULL) AND (check_in_longitude IS NOT NULL)))),
    CONSTRAINT ck_work_proof_records_check_in_range CHECK (((check_in_latitude IS NULL) OR (((check_in_latitude >= ('-90'::integer)::numeric) AND (check_in_latitude <= (90)::numeric)) AND ((check_in_longitude >= ('-180'::integer)::numeric) AND (check_in_longitude <= (180)::numeric))))),
    CONSTRAINT ck_work_proof_records_check_out_coords CHECK ((((check_out_latitude IS NULL) AND (check_out_longitude IS NULL)) OR ((check_out_latitude IS NOT NULL) AND (check_out_longitude IS NOT NULL)))),
    CONSTRAINT ck_work_proof_records_check_out_range CHECK (((check_out_latitude IS NULL) OR (((check_out_latitude >= ('-90'::integer)::numeric) AND (check_out_latitude <= (90)::numeric)) AND ((check_out_longitude >= ('-180'::integer)::numeric) AND (check_out_longitude <= (180)::numeric))))),
    CONSTRAINT ck_work_proof_records_checkout_payload CHECK (((check_out_server_at IS NULL) OR (check_out_device_at IS NOT NULL))),
    CONSTRAINT ck_work_proof_records_checkout_sequence CHECK (((check_out_device_at IS NULL) OR (check_in_device_at IS NULL) OR (check_out_device_at >= check_in_device_at))),
    CONSTRAINT ck_work_proof_records_live_tap_payload CHECK ((((source_type)::text <> 'LIVE_TAP'::text) OR ((check_in_device_at IS NOT NULL) AND (check_in_server_at IS NOT NULL) AND (check_in_latitude IS NOT NULL) AND (check_in_longitude IS NOT NULL)))),
    CONSTRAINT ck_work_proof_records_modification_count CHECK ((modification_count >= 0)),
    CONSTRAINT ck_work_proof_records_reflection_status CHECK (((reflection_status)::text = ANY ((ARRAY['PENDING'::character varying, 'REFLECTED'::character varying, 'NEEDS_REVIEW'::character varying, 'EXCLUDED'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_records_server_sequence CHECK (((check_out_server_at IS NULL) OR (check_in_server_at IS NULL) OR (check_out_server_at >= check_in_server_at))),
    CONSTRAINT ck_work_proof_records_source_type CHECK (((source_type)::text = ANY ((ARRAY['LIVE_TAP'::character varying, 'MANUAL_MISSING_RECORD'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_records_status CHECK (((record_status)::text = ANY ((ARRAY['CHECKED_IN'::character varying, 'CHECKED_OUT'::character varying, 'NEEDS_REVIEW'::character varying])::text[]))),
    CONSTRAINT ck_work_proof_records_status_payload CHECK (((((record_status)::text = 'CHECKED_IN'::text) AND (check_out_device_at IS NULL) AND (check_out_server_at IS NULL) AND (worked_minutes IS NULL)) OR (((record_status)::text = 'CHECKED_OUT'::text) AND (check_out_device_at IS NOT NULL) AND (check_out_server_at IS NOT NULL) AND (worked_minutes IS NOT NULL)) OR ((record_status)::text = 'NEEDS_REVIEW'::text))),
    CONSTRAINT ck_work_proof_records_worked_minutes CHECK (((worked_minutes IS NULL) OR (worked_minutes >= 0)))
);


--
-- Name: work_proof_records_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.work_proof_records ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.work_proof_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: workplaces; Type: TABLE; Schema: test; Owner: -
--

CREATE TABLE test.workplaces (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    address character varying(255) NOT NULL,
    map_label character varying(120),
    latitude numeric(9,6) NOT NULL,
    longitude numeric(9,6) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_workplaces_latitude CHECK (((latitude >= ('-90'::integer)::numeric) AND (latitude <= (90)::numeric))),
    CONSTRAINT ck_workplaces_longitude CHECK (((longitude >= ('-180'::integer)::numeric) AND (longitude <= (180)::numeric)))
);


--
-- Name: workplaces_id_seq; Type: SEQUENCE; Schema: test; Owner: -
--

ALTER TABLE test.workplaces ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME test.workplaces_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: advance_policies advance_policy_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_policies ALTER COLUMN advance_policy_id SET DEFAULT nextval('public.advance_policies_advance_policy_id_seq'::regclass);


--
-- Name: advance_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_requests ALTER COLUMN id SET DEFAULT nextval('public.advance_requests_id_seq'::regclass);


--
-- Name: claim_preparations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_preparations ALTER COLUMN id SET DEFAULT nextval('public.claim_preparations_id_seq'::regclass);


--
-- Name: companies id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq'::regclass);


--
-- Name: correction_decision_audits id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.correction_decision_audits ALTER COLUMN id SET DEFAULT nextval('public.correction_decision_audits_id_seq'::regclass);


--
-- Name: correction_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.correction_requests ALTER COLUMN id SET DEFAULT nextval('public.correction_requests_id_seq'::regclass);


--
-- Name: document_generation_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_generation_requests ALTER COLUMN id SET DEFAULT nextval('public.document_generation_requests_id_seq'::regclass);


--
-- Name: employer_invitation_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_invitation_tokens ALTER COLUMN id SET DEFAULT nextval('public.employer_invitation_tokens_id_seq'::regclass);


--
-- Name: employer_profiles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_profiles ALTER COLUMN id SET DEFAULT nextval('public.employer_profiles_id_seq'::regclass);


--
-- Name: employer_signup_codes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_signup_codes ALTER COLUMN id SET DEFAULT nextval('public.employer_signup_codes_id_seq'::regclass);


--
-- Name: employment_memberships id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_memberships ALTER COLUMN id SET DEFAULT nextval('public.employment_memberships_id_seq'::regclass);


--
-- Name: jobs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs ALTER COLUMN id SET DEFAULT nextval('public.jobs_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: vault_positions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_positions ALTER COLUMN id SET DEFAULT nextval('public.vault_positions_id_seq'::regclass);


--
-- Name: vault_yield_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_yield_logs ALTER COLUMN id SET DEFAULT nextval('public.vault_yield_logs_id_seq'::regclass);


--
-- Name: wage_deposits id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_deposits ALTER COLUMN id SET DEFAULT nextval('public.wage_deposits_id_seq'::regclass);


--
-- Name: wage_verifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verifications ALTER COLUMN id SET DEFAULT nextval('public.wage_verifications_id_seq'::regclass);


--
-- Name: work_contracts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_contracts ALTER COLUMN id SET DEFAULT nextval('public.work_contracts_id_seq'::regclass);


--
-- Name: work_proof_audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proof_audit_logs ALTER COLUMN id SET DEFAULT nextval('public.work_proof_audit_logs_id_seq'::regclass);


--
-- Name: work_proofs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proofs ALTER COLUMN id SET DEFAULT nextval('public.work_proofs_id_seq'::regclass);


--
-- Name: worker_registration_codes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_registration_codes ALTER COLUMN id SET DEFAULT nextval('public.worker_registration_codes_id_seq'::regclass);


--
-- Name: workplaces id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workplaces ALTER COLUMN id SET DEFAULT nextval('public.workplaces_id_seq'::regclass);


--
-- Name: 18312; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('18312');


--
-- Name: 18313; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('18313');


--
-- Name: 18314; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('18314');


--
-- Name: 18315; Type: BLOB; Schema: -; Owner: -
--

SELECT pg_catalog.lo_create('18315');


--
-- Data for Name: advance_payouts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.advance_payouts (advance_payout_id, advance_request_id, amount_atomic, asset_symbol, created_at, failure_reason, idempotency_key, signed_transaction, status, tx_hash, updated_at, user_id, wallet_address) FROM stdin;
ap_7ee7f2caa6c447bc	4	34000000	dUSDC	2026-03-27 01:13:52.275834	\N	advance-payout:4	\N	CONFIRMED	0x39335050529de99fcc1446e398f31d91c078bed14ef194c316528f026c59862d	2026-03-27 01:14:00.548345	51	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06
\.


--
-- Data for Name: advance_policies; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.advance_policies (advance_policy_id, asset_decimals, asset_symbol, created_at, disclaimer, enabled, fee_type, flat_fee_display_krw_amount, manual_repayment_enabled, max_cap_display_krw_amount, near_payday_max_cap_display_krw_amount, payday_day, reduced_cap_days_before_payday, reference_krw_per_asset, same_day_advance_allowed, settlement_mode, updated_at) FROM stdin;
1	6	dUSDC	2026-03-25 16:33:22.157138	미리받기 금액은 반영된 근무 기록 기준의 데모 시뮬레이션입니다. 실제 금융 서비스 제공을 의미하지 않습니다.	t	FLAT	5000	f	500000	50000	31	7	1450.00	f	PAYDAY_AUTO_OFFSET	2026-03-26 23:28:54.208547
\.


--
-- Data for Name: advance_requests; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.advance_requests (repayment_due_date, snapshot_needs_review_record_count, snapshot_policy_rate, snapshot_reflected_work_days, year_month, approved_amount, contract_id, created_at, fee_amount, id, requested_amount, requested_at, snapshot_available_amount, snapshot_max_cap, snapshot_reflected_work_minutes, user_id, workplace_id, status, idempotency_key, reviewed_at, reviewed_by_account_id, approved_amount_atomic, approved_reference_krw, asset_decimals, asset_symbol, reference_exchange_rate, fee_amount_atomic, fee_reference_krw, requested_amount_atomic, requested_reference_krw, snapshot_available_amount_atomic, snapshot_available_reference_krw, snapshot_max_cap_amount_atomic, snapshot_max_cap_reference_krw) FROM stdin;
2026-03-31	0	0.20	18	2026-03	\N	7	2026-03-27 01:12:14.362007	\N	4	\N	2026-03-26 18:12:06	\N	\N	9731	51	16	APPROVED	android-e8d90dff-9f86-43c1-b34b-7efda6c2ec5a	2026-03-27 01:13:52.26596	3	34000000	49300	6	dUSDC	1450.00	3448275	5000	34000000	49300	34482758	50000	34482758	50000
\.


--
-- Data for Name: claim_preparations; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.claim_preparations (claim_kit_document_id, created_at, id, user_id, wage_verification_id, locale, status, tone, summary_text) FROM stdin;
\.


--
-- Data for Name: companies; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.companies (created_at, id, updated_at, status, company_code, name, default_workplace_id, overtime_rounding_unit, scheduled_clock_in_time, scheduled_clock_out_time) FROM stdin;
2026-03-26 09:34:27.46232	11	2026-03-26 09:34:27.501661	ACTIVE	DN-SEOUL-2914	돈던 물류	15	FIFTEEN_MINUTES	09:00:00	18:00:00
2026-03-26 15:21:44.925898	12	2026-03-26 15:21:44.972801	ACTIVE	JH-SEOUL-1234	제이에이치 주식회사	17	FIFTEEN_MINUTES	09:00:00	18:00:00
\.


--
-- Data for Name: correction_decision_audits; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.correction_decision_audits (actor_account_id, correction_request_id, created_at, id, after_status, before_status, reject_reason_code, decision_memo) FROM stdin;
4	32	2026-03-26 09:34:28.007948	21	APPROVED	PENDING	\N	회의 종료 후 추가 확인한 내용과 증빙이 일치해 수락했어요.
4	33	2026-03-26 09:34:28.035009	22	REJECTED	PENDING	MANUAL_ENTRY_MISMATCH	입력 시간과 증빙이 맞지 않아 이번 요청은 반려했어요.
\.


--
-- Data for Name: correction_requests; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.correction_requests (attachment_count, work_date, company_id, created_at, decision_at, decision_by_account_id, id, original_clock_in_at, original_clock_out_at, requested_by_account_id, requested_clock_in_at, requested_clock_out_at, updated_at, work_proof_id, worker_account_id, workplace_id, status, reject_reason_code, decision_memo, reason, request_memo, attachment_metadata_json, reason_code, review_reason_code) FROM stdin;
1	2026-03-25	11	2026-03-26 09:34:27.962195	\N	\N	31	2026-03-25 09:03:00	2026-03-25 18:01:00	47	2026-03-25 08:58:00	2026-03-25 18:12:00	2026-03-26 09:34:27.962195	81	47	15	PENDING	\N	\N	출근/퇴근 탭을 늦게 눌렀어요	출근 직후 장비 점검 때문에 바로 기록하지 못했어요.	[{"type":"DOCUMENT","fileName":"장비점검기록.pdf","fileRef":"seed://kiosk-log"}]	OTHER	\N
1	2026-03-24	11	2026-03-26 09:34:27.97929	2026-03-26 05:34:27.999885	4	32	2026-03-24 08:57:00	2026-03-24 17:11:00	48	2026-03-24 08:57:00	2026-03-24 18:05:00	2026-03-26 09:34:28.003136	82	48	15	APPROVED	\N	회의 종료 후 추가 확인한 내용과 증빙이 일치해 수락했어요.	회의 정리 때문에 퇴근 입력이 늦었어요	팀 회의 마감 후 정리 때문에 늦게 입력했어요.	[{"type":"MEMO","fileName":"운영메모.txt","fileRef":"seed://manager-note"}]	OTHER	\N
0	2026-03-23	11	2026-03-26 09:34:28.022088	2026-03-26 07:34:28.026627	4	33	2026-03-23 09:14:00	2026-03-23 18:09:00	49	2026-03-23 08:45:00	2026-03-23 18:20:00	2026-03-26 09:34:28.030622	83	49	15	REJECTED	MANUAL_ENTRY_MISMATCH	입력 시간과 증빙이 맞지 않아 이번 요청은 반려했어요.	수기 입력 시간이 실제와 달랐어요	수기 입력으로 남긴 시간이라 정확하지 않을 수 있어요.	\N	OTHER	\N
\.


--
-- Data for Name: document_generation_requests; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.document_generation_requests (end_date, include_attachments, start_date, year_month, created_at, generated_at, id, updated_at, user_id, wage_verification_id, workplace_id, output_format, status, document_type, request_id, failure_reason, idempotency_key, file_name) FROM stdin;
2026-02-28	f	2026-02-01	\N	2026-03-23 11:02:15.995261	2026-03-23 11:02:19.619752	1	2026-03-23 11:02:19.624485	2	\N	1	PDF	READY	WORKPROOF_STATEMENT	063cf71b-d6c9-4a8d-ace6-d7ded1e3d420	\N	android-workproof-7b63516e-e6e3-4e0a-92b2-3c8655963596	workproof-2026-02-01-2026-02-28.pdf
2026-03-31	f	2026-03-01	\N	2026-03-27 00:05:27.869451	\N	2	2026-03-27 00:05:27.869451	51	\N	16	PDF	QUEUED	WORKPROOF_STATEMENT	a3bde753-7639-4546-a36a-9605dec6eb6d	\N	android-workproof-6ff622fe-75b4-4206-9b48-c36d24adad6f	\N
2026-03-31	f	2026-03-01	\N	2026-03-27 00:25:45.050135	2026-03-27 00:25:50.773824	3	2026-03-27 00:25:50.784211	51	\N	16	PDF	READY	WORKPROOF_STATEMENT	b6cae248-6f74-4f78-92ad-34467a088968	\N	android-workproof-e23910ed-a7f0-489d-bd3e-97194988ca5d	workproof-2026-03-01-2026-03-31.pdf
2026-03-31	f	2026-03-01	\N	2026-03-27 00:26:49.721202	2026-03-27 00:26:52.638429	4	2026-03-27 00:26:52.645906	51	\N	16	PDF	READY	WORKPROOF_STATEMENT	1e2d943d-87ab-4230-856a-cdfb722d12c6	\N	android-workproof-e379e1f7-6a76-4993-b6ef-08fab1f9146e	workproof-2026-03-01-2026-03-31.pdf
\.


--
-- Data for Name: employer_invitation_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employer_invitation_tokens (company_id, created_at, default_workplace_id, expires_at, id, invited_by_account_id, revoked_at, updated_at, used_at, role, token_hash, invitee_email) FROM stdin;
\.


--
-- Data for Name: employer_profiles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employer_profiles (account_id, company_id, created_at, default_workplace_id, id, updated_at, status, display_name) FROM stdin;
4	11	2026-03-26 09:34:27.526283	15	11	2026-03-26 09:34:27.526283	ACTIVE	돈던 관리자
52	12	2026-03-26 15:22:18.598634	17	12	2026-03-26 15:22:18.598634	ACTIVE	차지훈
\.


--
-- Data for Name: employer_signup_codes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employer_signup_codes (id, created_at, updated_at, code_hash, company_id, default_workplace_id, encrypted_code, issued_by_account_id, revoked_at) FROM stdin;
11	2026-03-26 09:34:27.518186	2026-03-26 09:34:27.518186	a2e6c8348db597997814de8c7f37ad1fc579f31890c3b4218c4c5faf6313b3ba	11	15	mQBqHe0d2wz9cJ1LuQwINbDrva0QiAMQUFP2Lba3yHsZd4YjnZGXTVR4YVjr3lo=	4	\N
12	2026-03-26 15:21:44.963494	2026-03-26 15:21:44.963494	63aad3e59fb1df84c5ca228df067c3eede0234ef4b9a32035dfda17ca7bf1ab1	12	17	6g/5sq3CoT3MtC2surkYeUMQNmpExOv+4OChTY2U1gA8Yf4KELVrSMs+AOtX5g==	3	\N
\.


--
-- Data for Name: employment_memberships; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employment_memberships (effective_from, effective_to, company_id, created_at, id, updated_at, worker_account_id, workplace_id, status) FROM stdin;
2026-01-01	\N	11	2026-03-26 09:34:27.859678	42	2026-03-26 09:34:27.859678	47	15	ACTIVE
2026-01-01	\N	11	2026-03-26 09:34:27.866064	43	2026-03-26 09:34:27.866064	48	15	ACTIVE
2026-01-01	\N	11	2026-03-26 09:34:27.869254	44	2026-03-26 09:34:27.869254	49	15	ACTIVE
2026-01-01	\N	11	2026-03-26 09:34:27.87421	45	2026-03-26 09:34:27.87421	50	15	ACTIVE
2026-03-26	\N	11	2026-03-26 09:44:10.723475	46	2026-03-26 09:44:10.723475	1	15	ACTIVE
2026-03-26	\N	12	2026-03-26 15:28:21.991913	47	2026-03-26 15:28:21.991913	51	17	ACTIVE
\.


--
-- Data for Name: jobs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.jobs (attempt_count, created_at, id, run_at, updated_at, status, job_type, reference_kind, reference_id, active_key, last_error) FROM stdin;
1	2026-03-23 12:04:43.390954	1	2026-03-23 12:04:43.390424	2026-03-23 12:04:45.377721	DONE	SUBMIT_TRANSFER	TRANSFER	tr_cdb07bf19d184a07	\N	\N
1	2026-03-26 15:47:38.957073	13	2026-03-26 15:47:38.957073	2026-03-26 15:47:39.837107	DONE	SUBMIT_TRANSFER	TRANSFER	tr_ae2819fa4c0a4dcd	\N	\N
1	2026-03-26 15:48:56.364857	15	2026-03-26 15:48:56.364857	2026-03-26 15:48:57.002436	DONE	SUBMIT_TRANSFER	TRANSFER	tr_7b6913dd245b4068	\N	\N
2	2026-03-23 12:04:45.373515	2	2026-03-23 12:04:49.510103	2026-03-23 12:04:49.633129	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_cdb07bf19d184a07	\N	\N
1	2026-03-23 16:06:31.829757	3	2026-03-23 16:06:31.829757	2026-03-23 16:06:33.985351	DONE	SUBMIT_TRANSFER	TRANSFER	tr_8cad60941e3f441c	\N	\N
2	2026-03-23 16:06:33.980354	4	2026-03-23 16:06:38.118036	2026-03-23 16:06:38.235112	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_8cad60941e3f441c	\N	\N
5	2026-03-26 15:48:56.972919	16	2026-03-26 15:48:59.738255	2026-03-26 15:49:00.421426	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_7b6913dd245b4068	\N	\N
1	2026-03-25 16:53:43.594556	5	2026-03-25 16:53:43.593619	2026-03-25 16:53:45.377786	DONE	SUBMIT_VAULT_TRANSACTION	VAULT	vtx_adeed3ff2b49468e	\N	\N
1	2026-03-25 16:53:45.357352	6	2026-03-25 16:53:47.356602	2026-03-25 16:53:47.478578	DONE	POLL_VAULT_TRANSACTION_RECEIPT	VAULT	vtx_adeed3ff2b49468e	\N	\N
1	2026-03-26 16:19:09.032357	17	2026-03-26 16:19:09.032357	2026-03-26 16:19:09.587158	DONE	SUBMIT_TRANSFER	TRANSFER	tr_a02a85c121044072	\N	\N
1	2026-03-26 15:43:00.687206	7	2026-03-26 15:43:00.686103	2026-03-26 15:43:01.267423	DONE	SUBMIT_TRANSFER	TRANSFER	tr_bff4b3a3c5cf451c	\N	\N
5	2026-03-26 16:19:09.577482	18	2026-03-26 16:19:12.178695	2026-03-26 16:19:12.833924	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_a02a85c121044072	\N	\N
1	2026-03-27 00:24:38.710441	19	2026-03-27 00:24:38.706415	2026-03-27 00:24:41.756666	DONE	SUBMIT_VAULT_TRANSACTION	VAULT	vtx_7ca77786a97f4831	\N	\N
1	2026-03-27 00:24:41.741708	20	2026-03-27 00:24:43.741708	2026-03-27 00:24:43.855066	DONE	POLL_VAULT_TRANSACTION_RECEIPT	VAULT	vtx_7ca77786a97f4831	\N	\N
1	2026-03-27 01:13:52.320125	21	2026-03-27 01:13:52.319008	2026-03-27 01:13:53.127553	DONE	SUBMIT_ADVANCE_PAYOUT	ADVANCE_PAYOUT	ap_7ee7f2caa6c447bc	\N	\N
17	2026-03-26 15:43:01.259184	8	2026-03-26 15:43:11.98302	2026-03-26 15:43:12.64767	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_bff4b3a3c5cf451c	\N	\N
1	2026-03-26 15:43:22.047284	9	2026-03-26 15:43:22.047284	2026-03-26 15:43:22.886633	DONE	SUBMIT_TRANSFER	TRANSFER	tr_83916a7a457546e4	\N	\N
10	2026-03-27 01:13:53.117969	22	2026-03-27 01:13:59.731285	2026-03-27 01:14:00.548345	DONE	POLL_ADVANCE_PAYOUT_RECEIPT	ADVANCE_PAYOUT	ap_7ee7f2caa6c447bc	\N	\N
31	2026-03-26 15:47:39.830146	14	2026-03-26 15:47:59.682667	2026-03-26 15:48:00.366943	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_ae2819fa4c0a4dcd	\N	\N
21	2026-03-26 15:43:22.867166	10	2026-03-26 15:43:36.139703	2026-03-26 15:43:36.80238	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_83916a7a457546e4	\N	\N
1	2026-03-26 15:43:52.28226	11	2026-03-26 15:43:52.281738	2026-03-26 15:43:52.847899	DONE	SUBMIT_TRANSFER	TRANSFER	tr_9ae8c47e1f614897	\N	\N
12	2026-03-26 15:43:52.838287	12	2026-03-26 15:44:00.242992	2026-03-26 15:44:00.902723	DONE	POLL_TRANSFER_RECEIPT	TRANSFER	tr_9ae8c47e1f614897	\N	\N
\.


--
-- Data for Name: recipients; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recipients (allowed, created_at, target_user_id, updated_at, user_id, relation, wallet_address, recipient_id, alias) FROM stdin;
t	2026-03-23 12:04:20.153598	17	2026-03-23 12:04:20.153598	2	FAMILY	0x00f50076f1f2627b71417d9b19be08543071c49c	rcp_7c1f692a0134	jihun
t	2026-03-26 15:41:15.171839	51	2026-03-26 15:41:15.171839	53	FAMILY	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	rcp_0dec837f5d97	이나은
t	2026-03-26 16:15:06.756098	53	2026-03-26 16:15:06.756098	51	FRIEND	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	rcp_e07f614e0e6d	이수빈
t	2026-03-26 16:19:01.435107	54	2026-03-26 16:19:01.435107	51	FRIEND	0xe7fa8815aee4e66d25d1f1be9c475218792bb63e	rcp_9ae9d225b701	한지훈
\.


--
-- Data for Name: transfers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.transfers (high_amount_confirmed, recent_recipient_confirmed, amount_atomic, created_at, recipient_target_user_id_snapshot, updated_at, user_id, asset_symbol, recipient_relation_snapshot, status, failure_code, recipient_address, sender_address, recipient_id, transfer_id, tx_hash, recipient_alias_snapshot, idempotency_key, signed_transaction, network_fee_wei) FROM stdin;
f	t	1000000	2026-03-23 12:04:43.387254	17	2026-03-23 12:04:49.633129	2	dUSDC	FAMILY	CONFIRMED	\N	0x00f50076f1f2627b71417d9b19be08543071c49c	0xbb60385de83848e5791ce871c429172626744e5f	rcp_7c1f692a0134	tr_cdb07bf19d184a07	0xd1c43cbb5508c46bc831745e35a8192597bd45463d73f7b791f954e95c700a9c	jihun	android-remit-ca8984ea-27f5-4b91-b673-5c6580e77606	\N	34434482076
f	t	1000000	2026-03-23 16:06:31.826647	17	2026-03-23 16:06:38.235112	2	dUSDC	FAMILY	CONFIRMED	\N	0x00f50076f1f2627b71417d9b19be08543071c49c	0xbb60385de83848e5791ce871c429172626744e5f	rcp_7c1f692a0134	tr_8cad60941e3f441c	0xaef4967daefc35082013e6e6261f0dc41ca0d737ceeb72a51271a26fb2ed69ba	jihun	android-remit-dc110091-bb67-4748-a430-66afa9056fac	\N	34434619812
f	t	270000000	2026-03-26 15:43:00.67803	51	2026-03-26 15:43:12.64767	53	dUSDC	FAMILY	CONFIRMED	\N	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	rcp_0dec837f5d97	tr_bff4b3a3c5cf451c	0x7241547d85ea94630456ddaa026294e18dbe971d0bc74994f01b67b3d5824f9f	이나은	android-remit-114b3fbf-c29f-4f14-a755-619950f18a6e	\N	34470723870
f	t	300000000	2026-03-26 15:43:22.041502	51	2026-03-26 15:43:36.80238	53	dUSDC	FAMILY	CONFIRMED	\N	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	rcp_0dec837f5d97	tr_83916a7a457546e4	0x1192b33db739a8780ccce8bcb9df4b524924e738fa250efd9657ddb0b4c7ad49	이나은	android-remit-837be0f3-df5b-4339-b343-d7ae4f572804	\N	34458689160
f	t	300000000	2026-03-26 15:43:52.278238	51	2026-03-26 15:44:00.902723	53	dUSDC	FAMILY	CONFIRMED	\N	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	rcp_0dec837f5d97	tr_9ae8c47e1f614897	0xb9f0930d4f93c3ba05dcac080a975c8fe671d4e0c7c308ee3409e1a66c36bcf6	이나은	android-remit-1164cadd-9752-4c5e-88dc-8e9ca33e44f9	\N	34458654702
f	t	300000000	2026-03-26 15:47:38.953963	51	2026-03-26 15:48:00.366943	53	dUSDC	FAMILY	CONFIRMED	\N	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	rcp_0dec837f5d97	tr_ae2819fa4c0a4dcd	0x6d5c09340381987e6f2461ebd2af563486fa9d0928c40ff2d0f4909bb301dd85	이나은	android-remit-6a8b9a69-3f48-46f3-a375-7ba2f0256892	\N	34458723618
f	t	30000000	2026-03-26 15:48:56.35954	51	2026-03-26 15:49:00.421426	53	dUSDC	FAMILY	CONFIRMED	\N	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	rcp_0dec837f5d97	tr_7b6913dd245b4068	0x31c9708d0782c23ec18a2271b9ee16739a5a2b7feaa6ab0032cae7e257942732	이나은	android-remit-e4673b65-48d9-44a5-8d03-e520ea2ffea6	\N	34470654930
f	t	50000000	2026-03-26 16:19:09.027569	54	2026-03-26 16:19:12.833924	51	dUSDC	FRIEND	CONFIRMED	\N	0xe7fa8815aee4e66d25d1f1be9c475218792bb63e	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	rcp_9ae9d225b701	tr_a02a85c121044072	0x93f800c83986f9cf8e1ff2e06e48828a958ccc14f73ae496f09ff4627e7005aa	한지훈	android-remit-67818f65-18b7-42b3-bd65-f602bd48822f	\N	34471309860
\.


--
-- Data for Name: user_wallets; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_wallets (created_at, funded_at, updated_at, user_id, funding_status, wallet_address, funding_failure_reason, encrypted_private_key) FROM stdin;
2026-03-20 16:53:53.682136	2026-03-20 16:54:13.331561	2026-03-20 16:54:13.334194	1	FUNDED	0xc80817b759d05d41c5f693b8338960c70d9da685	\N	6ukqGGlHmxCPNmwrl3m7hteqr/cYuk9BuprJCHnjV33zsAvKaSBke9GoMfgjkZeucd7sdKiz+EUH/LIKF+dgwhs0Hfwzc3A1r3FpDzmafhBpI53UDIlTfbvut80=
2026-03-23 09:26:29.948908	2026-03-23 09:26:49.70719	2026-03-23 09:26:49.710465	17	FUNDED	0x00f50076f1f2627b71417d9b19be08543071c49c	\N	r3lZ0uDEe2GU27pavr1OfAGiugrw74pxAmJn5A0KB27HTDEK3YP3qHwMS8GJb3Lsbt/V7RgbQc2KpbuxPu5U+eIYqnI6PlRK+4fwf1cx9CSdzvH3i4pfrie1V3s=
2026-03-23 10:55:06.082043	2026-03-23 10:56:14.778177	2026-03-23 10:56:14.779861	18	FUNDED	0x00cc69727e9663f5e8631f85da1e10e69d8cf5c5	\N	dA6VKMbc7d8sPWMu9UcBiSORk9tu2i1AZ6z0HN3TseHm9cKV63wUrDgMgORLMODldjbHJ1fKSDVw5im9xn35j50ZpwiJVOeRqytZvVZrk0XspIB0oSxx8AmxAjw=
2026-03-23 11:01:59.732807	2026-03-23 11:02:14.063032	2026-03-23 11:02:14.06408	2	FUNDED	0xbb60385de83848e5791ce871c429172626744e5f	\N	w7SpFGDxEZicXyH2531Y8QBxt46KbHboDEj3FsKlVDoqXMj1A8ENkMgUj7xFDLRE85khQvVdDqkQqHMf8jY1ltO8jVtA7KTSiGXoK4yyeQBrf075s1zn+xN1eAI=
2026-03-26 15:20:04.465494	2026-03-26 15:20:24.699858	2026-03-26 15:20:24.702104	51	FUNDED	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06	\N	8IxxpqPqnZWhz3pUcaV3FzIGiEJA8jwxoyexNFsEsI7ozSv0+I3DK+2NCkcn6PhRvsfOp02qI4aycaQfgK21zEKLXwysIkjbo2k1Thnf3V8BTbdZJd7J2KwLOAs=
2026-03-26 15:35:00.971063	2026-03-26 15:35:24.863273	2026-03-26 15:35:24.87012	53	FUNDED	0x33e15f96b4a5cd37433a709a0831cbcb2d13fce4	\N	oHCM1Feby7dq9nAfkG5ci/omfPzhs/pX3MviToRFGqNr0oaH2T+UtGgU39simkmC+3ZPFVCua/l9NxJhpJjlkFwbFQxsd7zqqxN9L0H6039gfyEBsAYr2rHq+DI=
2026-03-26 16:17:51.370384	2026-03-26 16:18:12.905748	2026-03-26 16:18:12.909446	54	FUNDED	0xe7fa8815aee4e66d25d1f1be9c475218792bb63e	\N	8ERdu/f22JpsHWKJECEfPf46XEuftzd/SmS6EyE5QD81cyMazeg/o1Us1OnpWKdo/fvHggFa8ZPibMxC/QBeLXkcKPweaHnW+RFye6V1YYvfkX40hv4u04+zn3o=
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (created_at, id, updated_at, company_code, phone_number, role, name, email, password_hash) FROM stdin;
2026-03-20 16:48:40.31531	1	2026-03-20 16:48:40.31531	DONDONE2026	01012345678	USER	Test User	test@gmail.com	$2a$10$qQlZGKwwarqiEBvLUSRpy.KBlqLLN0XXvzVyU30aGijxB3LOIWwfa
2026-03-20 16:48:40.410893	2	2026-03-20 16:48:40.410893	DONDONE2026	01098765432	USER	PDF Demo User	demo@test.com	$2a$10$.xWumbaOCaheVRaocK/ZAOcr7QS30bcNKHPTp95hfwf0deQHYZBrW
2026-03-23 09:03:09.970596	3	2026-03-23 09:03:09.970596	\N	\N	ADMIN	서비스 관리자	admin@dondone.local	$2a$10$idEajihElxz8fk/wO3ow1uMYCIvsIv1clNhsICRg759Z64DgPndP2
2026-03-23 09:26:28.844706	17	2026-03-23 09:26:28.844706	\N	01012341234	USER	jihun	jihun@gmail.com	$2a$10$mB1xOS4Hquij3lhfOa/WV.x7OeSSSF8WWJXBfTCYQXNaWL/Dyu4XS
2026-03-23 10:55:05.796157	18	2026-03-23 10:55:05.796157	\N	01056781234	USER	chajihun	chajihun@gmail.com	$2a$10$60su8T7HBFvZ9xJPwb1TxO1mSXZoydhFCDG.Jf6v7dqw8KVn/xB9a
2026-03-26 09:34:27.603557	47	2026-03-26 09:34:27.603557	DONDONE2026	01020000001	USER	김민수	worker.complete@acme.test	$2a$10$33ChoHoZhKfQLnk4fkGC/u9./TPEssCeo4PR9cG76HxrfhRMdWKGO
2026-03-26 09:34:27.690951	48	2026-03-26 09:34:27.690951	DONDONE2026	01020000002	USER	이서연	worker.working@acme.test	$2a$10$xaZFRbgVZQE.S.6iG5YVXOfuwVIHF7awgs/obFzLcvdUe7KGxhDiG
2026-03-26 09:34:27.764231	49	2026-03-26 09:34:27.764231	DONDONE2026	01020000003	USER	박준호	worker.review@acme.test	$2a$10$GwKQMOmqFhmHwxsWVlz9XeEUsFWvMoNLkrCnS8IEnYSg1JdIiDabC
2026-03-26 09:34:27.851201	50	2026-03-26 09:34:27.851201	DONDONE2026	01020000004	USER	정하늘	worker.norecord@acme.test	$2a$10$.NHJ83PB9Ptapl.iGmNNn.0nOQSnDip6hEC/.0WqzcS4BqyWoy0XC
2026-03-23 09:03:10.214176	4	2026-03-26 14:35:15.84284	\N	\N	EMPLOYER	돈돈 관리자	manager@gmail.com	$2a$10$TsAAteRS2TabJhDL1ENIv.xsRn16ZdZSTxtez069aArUtqQbv3K8y
2026-03-26 15:20:03.351883	51	2026-03-26 15:20:03.351883	\N	01011112222	USER	이나은	leenaeun98@gmail.com	$2a$10$UsJzpf7ykKQArdLW0WJVIOQDe3pHEE96yHn.AJW3HesAxzN.g.F.6
2026-03-26 15:22:18.593274	52	2026-03-26 15:22:18.593274	\N	\N	EMPLOYER	차지훈	jihun1234@gmail.com	$2a$10$VajR5HbhxSJ2Gttdzj6aTujyq/CQqmnGWWgTixNLPEZgD92ialNDW
2026-03-26 15:35:00.37107	53	2026-03-26 15:35:00.37107	\N	01012341111	USER	이수빈	leesubin1234@gmail.com	$2a$10$o4.EyCD1IB0TCjb.AIh5rOdWdfS.hxzKkhpD.Xw8G7muPJelrAYnG
2026-03-26 16:17:51.181259	54	2026-03-26 16:17:51.181259	\N	01012347410	USER	한지훈	hanjihun1234@gmail.com	$2a$10$2UeQyRaYUoMk3pxmn2I46OpwjJgaxxbyVUmjBcGao1ddCd5rimfWu
\.


--
-- Data for Name: vault_positions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vault_positions (id, accrued_yield_atomic, apy_bps, asset_address, asset_symbol, created_at, last_accrued_at, network, principal_amount_atomic, share_balance, status, updated_at, user_id, vault_address, wallet_address) FROM stdin;
1	1229	500	0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f	dUSDC	2026-03-25 16:53:43.296838	2026-03-26 14:36:21.156947	sepolia	10000000	10000000	ACTIVE	2026-03-26 14:36:21.180705	1	0x369fba70c929c25b6fa43b026f0504c76408bae7	0xc80817b759d05d41c5f693b8338960c70d9da685
2	230	500	0xa12993fda23a1f18f1c9f0cd76f0a27c1cc20d8f	dUSDC	2026-03-27 00:24:37.394294	2026-03-27 01:14:11.176673	sepolia	50000000	50000000	ACTIVE	2026-03-27 01:14:11.182955	51	0x369fba70c929c25b6fa43b026f0504c76408bae7	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06
\.


--
-- Data for Name: vault_transactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vault_transactions (vault_transaction_id, amount_atomic, asset_symbol, confirmed_at, created_at, failure_code, idempotency_key, position_id, share_delta, signed_transaction, status, tx_hash, tx_type, updated_at, user_id, vault_address, wallet_address) FROM stdin;
vtx_adeed3ff2b49468e	10000000	dUSDC	2026-03-25 16:53:47.478578	2026-03-25 16:53:43.580584	\N	android-vault-deposit-3070a93e-c886-4c92-b7d2-805d3ab58eba	1	10000000	\N	CONFIRMED	0xc58fbb6c304d8f63087a616edcbccb7defa8c5fbab3f6268be5755c365fcbf01	DEPOSIT	2026-03-25 16:53:47.479338	1	0x369fba70c929c25b6fa43b026f0504c76408bae7	0xc80817b759d05d41c5f693b8338960c70d9da685
vtx_7ca77786a97f4831	50000000	dUSDC	2026-03-27 00:24:43.855066	2026-03-27 00:24:38.680302	\N	android-vault-deposit-9635b69b-fdfc-4863-a2c3-46b77d266f22	2	50000000	\N	CONFIRMED	0x2c4167f265ba227a9fe32d1ccb943f5e0cfbd0663b2f03023b32b7a83052a85b	DEPOSIT	2026-03-27 00:24:43.855066	51	0x369fba70c929c25b6fa43b026f0504c76408bae7	0x4dd8ddfe592c0f730fea4d1d6cdb80331d606c06
\.


--
-- Data for Name: vault_yield_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vault_yield_logs (id, apy_bps, calculation_basis, created_at, from_at, position_id, principal_amount_atomic, to_at, user_id, yield_amount_atomic) FROM stdin;
1	500	SECONDLY_SIMPLE	2026-03-25 17:13:50.855223	2026-03-25 16:53:48.811404	1	10000000	2026-03-25 17:13:50.853571	1	19
2	500	SECONDLY_SIMPLE	2026-03-25 17:16:21.289899	2026-03-25 17:13:50.853571	1	10000000	2026-03-25 17:16:21.289899	1	2
3	500	SECONDLY_SIMPLE	2026-03-25 17:17:56.464633	2026-03-25 17:16:21.289899	1	10000000	2026-03-25 17:17:56.464081	1	1
4	500	SECONDLY_SIMPLE	2026-03-25 17:22:16.854109	2026-03-25 17:17:56.464081	1	10000000	2026-03-25 17:22:16.854109	1	4
5	500	SECONDLY_SIMPLE	2026-03-25 17:24:51.65058	2026-03-25 17:22:16.854109	1	10000000	2026-03-25 17:24:51.65002	1	2
6	500	SECONDLY_SIMPLE	2026-03-25 17:32:08.889241	2026-03-25 17:24:51.65002	1	10000000	2026-03-25 17:32:08.889241	1	6
7	500	SECONDLY_SIMPLE	2026-03-25 17:36:45.343344	2026-03-25 17:32:08.889241	1	10000000	2026-03-25 17:36:45.34257	1	4
8	500	SECONDLY_SIMPLE	2026-03-25 17:54:54.797609	2026-03-25 17:37:15.145247	1	10000000	2026-03-25 17:54:54.796958	1	16
9	500	SECONDLY_SIMPLE	2026-03-26 09:40:51.485687	2026-03-25 17:56:00.150311	1	10000000	2026-03-26 09:40:51.485154	1	898
10	500	SECONDLY_SIMPLE	2026-03-26 09:44:12.223939	2026-03-26 09:40:51.485154	1	10000000	2026-03-26 09:44:12.223612	1	3
11	500	SECONDLY_SIMPLE	2026-03-26 09:47:42.565911	2026-03-26 09:44:12.223612	1	10000000	2026-03-26 09:47:42.565369	1	3
12	500	SECONDLY_SIMPLE	2026-03-26 09:55:57.789257	2026-03-26 09:47:42.565369	1	10000000	2026-03-26 09:55:57.789257	1	7
13	500	SECONDLY_SIMPLE	2026-03-26 10:22:12.185786	2026-03-26 09:55:57.789257	1	10000000	2026-03-26 10:22:12.185786	1	24
14	500	SECONDLY_SIMPLE	2026-03-26 10:28:03.712125	2026-03-26 10:22:31.027586	1	10000000	2026-03-26 10:28:03.712125	1	5
15	500	SECONDLY_SIMPLE	2026-03-26 10:37:43.003874	2026-03-26 10:28:03.712125	1	10000000	2026-03-26 10:37:43.003874	1	9
16	500	SECONDLY_SIMPLE	2026-03-26 11:02:04.775476	2026-03-26 10:37:43.003874	1	10000000	2026-03-26 11:02:04.774797	1	23
17	500	SECONDLY_SIMPLE	2026-03-26 14:36:21.159615	2026-03-26 11:02:04.774797	1	10000000	2026-03-26 14:36:21.156947	1	203
18	500	SECONDLY_SIMPLE	2026-03-27 00:32:36.559755	2026-03-27 00:24:45.307549	2	50000000	2026-03-27 00:32:36.558825	51	37
19	500	SECONDLY_SIMPLE	2026-03-27 00:35:55.653196	2026-03-27 00:32:36.558825	2	50000000	2026-03-27 00:35:55.653196	51	15
20	500	SECONDLY_SIMPLE	2026-03-27 00:37:13.553853	2026-03-27 00:35:55.653196	2	50000000	2026-03-27 00:37:13.553853	51	6
21	500	SECONDLY_SIMPLE	2026-03-27 00:37:43.110528	2026-03-27 00:37:13.553853	2	50000000	2026-03-27 00:37:43.109948	51	2
22	500	SECONDLY_SIMPLE	2026-03-27 00:41:33.009648	2026-03-27 00:37:43.109948	2	50000000	2026-03-27 00:41:33.00901	51	18
23	500	SECONDLY_SIMPLE	2026-03-27 01:02:36.867137	2026-03-27 00:41:33.00901	2	50000000	2026-03-27 01:02:36.86647	51	100
24	500	SECONDLY_SIMPLE	2026-03-27 01:03:28.462903	2026-03-27 01:02:36.86647	2	50000000	2026-03-27 01:03:28.462903	51	4
25	500	SECONDLY_SIMPLE	2026-03-27 01:04:15.560809	2026-03-27 01:03:28.462903	2	50000000	2026-03-27 01:04:15.560809	51	3
26	500	SECONDLY_SIMPLE	2026-03-27 01:10:59.187068	2026-03-27 01:04:15.560809	2	50000000	2026-03-27 01:10:59.186557	51	31
27	500	SECONDLY_SIMPLE	2026-03-27 01:12:35.182581	2026-03-27 01:10:59.186557	2	50000000	2026-03-27 01:12:35.182581	51	7
28	500	SECONDLY_SIMPLE	2026-03-27 01:14:11.176673	2026-03-27 01:12:35.182581	2	50000000	2026-03-27 01:14:11.176673	51	7
\.


--
-- Data for Name: wage_deposits; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.wage_deposits (deductions_known, deposit_date, year_month, actual_deposit_amount, created_at, id, updated_at, user_id, note) FROM stdin;
f	2026-03-19	2026-03	1740000	2026-03-23 14:38:05.842941	1	2026-03-23 14:38:05.842941	2	\N
f	2026-03-19	2026-03	1740000	2026-03-23 15:11:36.430801	2	2026-03-23 15:11:36.430801	2	\N
f	2026-03-19	2026-03	1740000	2026-03-23 15:11:38.131178	3	2026-03-23 15:11:38.131178	2	\N
f	2026-03-19	2026-03	550000	2026-03-23 15:44:32.040386	4	2026-03-23 15:44:32.040386	2	\N
f	2026-03-19	2026-03	550000	2026-03-23 16:00:47.556134	5	2026-03-23 16:00:47.556134	2	\N
f	2026-03-19	2026-03	550000	2026-03-23 16:39:17.737376	6	2026-03-23 16:39:17.737376	2	\N
f	2026-03-19	2026-03	1100000	2026-03-23 16:42:56.894671	7	2026-03-23 16:42:56.894671	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 16:45:25.388994	8	2026-03-23 16:45:25.388994	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:45:31.61179	9	2026-03-23 16:45:31.61179	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 16:45:35.811187	10	2026-03-23 16:45:35.811187	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 16:46:19.460325	11	2026-03-23 16:46:19.460325	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 16:46:24.698287	12	2026-03-23 16:46:24.698287	2	\N
f	2026-03-19	2026-03	660000	2026-03-23 16:46:28.345677	13	2026-03-23 16:46:28.345677	2	\N
f	2026-03-19	2026-03	660000	2026-03-23 16:48:28.450995	14	2026-03-23 16:48:28.450995	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 16:48:34.962098	15	2026-03-23 16:48:34.962098	2	\N
f	2026-03-19	2026-03	910000	2026-03-23 16:48:41.318274	16	2026-03-23 16:48:41.318274	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 16:48:50.943416	17	2026-03-23 16:48:50.943416	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:48:57.271399	18	2026-03-23 16:48:57.271399	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:50:13.400357	19	2026-03-23 16:50:13.400357	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:51:22.847916	20	2026-03-23 16:51:22.847916	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:52:14.058072	21	2026-03-23 16:52:14.058072	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:52:31.809557	22	2026-03-23 16:52:31.809557	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:52:32.117283	23	2026-03-23 16:52:32.117283	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:52:32.292765	24	2026-03-23 16:52:32.292765	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:52:32.453364	25	2026-03-23 16:52:32.453364	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:53:15.836079	26	2026-03-23 16:53:15.836079	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:54:53.745939	27	2026-03-23 16:54:53.745939	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:55:38.211742	28	2026-03-23 16:55:38.211742	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 16:55:45.250199	29	2026-03-23 16:55:45.250199	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:02:57.888881	30	2026-03-23 17:02:57.888881	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:03:05.326811	31	2026-03-23 17:03:05.326811	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:03:10.121977	32	2026-03-23 17:03:10.121977	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:10:08.124778	33	2026-03-23 17:10:08.124778	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 17:10:14.143768	34	2026-03-23 17:10:14.143768	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 17:10:16.306813	35	2026-03-23 17:10:16.306813	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 17:10:16.465279	36	2026-03-23 17:10:16.465279	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 17:10:23.511606	37	2026-03-23 17:10:23.511606	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 17:10:31.93734	38	2026-03-23 17:10:31.93734	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 17:10:32.400152	39	2026-03-23 17:10:32.400152	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 17:10:32.674707	40	2026-03-23 17:10:32.674707	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 17:10:32.95651	41	2026-03-23 17:10:32.95651	2	\N
f	2026-03-19	2026-03	860000	2026-03-23 17:10:33.25442	42	2026-03-23 17:10:33.25442	2	\N
f	2026-03-19	2026-03	760000	2026-03-23 17:10:45.929506	43	2026-03-23 17:10:45.929506	2	\N
f	2026-03-19	2026-03	760000	2026-03-23 17:13:16.085269	44	2026-03-23 17:13:16.085269	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:13:31.689287	45	2026-03-23 17:13:31.689287	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 17:13:36.211883	46	2026-03-23 17:13:36.211883	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 17:15:02.477632	47	2026-03-23 17:15:02.477632	2	\N
f	2026-03-19	2026-03	910000	2026-03-23 17:16:32.534982	48	2026-03-23 17:16:32.534982	2	\N
f	2026-03-19	2026-03	910000	2026-03-23 17:17:10.193144	49	2026-03-23 17:17:10.193144	2	\N
f	2026-03-19	2026-03	910000	2026-03-23 17:18:30.282359	50	2026-03-23 17:18:30.282359	2	\N
f	2026-03-19	2026-03	910000	2026-03-23 17:23:05.039131	51	2026-03-23 17:23:05.039131	2	\N
f	2026-03-19	2026-03	810000	2026-03-23 17:23:23.197728	52	2026-03-23 17:23:23.197728	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:23:28.855286	53	2026-03-23 17:23:28.855286	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:23:29.166556	54	2026-03-23 17:23:29.166556	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:23:30.989542	55	2026-03-23 17:23:30.989542	2	\N
f	2026-03-19	2026-03	710000	2026-03-23 17:27:31.497527	56	2026-03-23 17:27:31.497527	2	\N
f	2026-03-26	2026-03	1740000	2026-03-27 00:05:09.009726	57	2026-03-27 00:05:09.009726	51	\N
f	2026-03-26	2026-03	1740000	2026-03-27 00:14:21.253333	58	2026-03-27 00:14:21.253333	51	\N
f	2026-03-26	2026-03	1740000	2026-03-27 00:18:33.097747	59	2026-03-27 00:18:33.097747	51	\N
f	2026-03-26	2026-03	1740000	2026-03-27 00:25:36.277095	60	2026-03-27 00:25:36.277095	51	\N
f	2026-03-26	2026-03	1800000	2026-03-27 00:26:36.161205	61	2026-03-27 00:26:36.161205	51	\N
\.


--
-- Data for Name: wage_verification_possible_causes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.wage_verification_possible_causes (cause_order, verification_id, cause_code, cause_title, cause_detail) FROM stdin;
0	1	OVERTIME_INCLUDED	연장 근무가 추정에 포함됐습니다	하루 8시간을 초과한 reflected 근무가 참고용 추정 급여에 반영됐습니다.
1	1	DIFFERENCE_OVER_THRESHOLD	확인 필요한 차이가 감지됐습니다	참고용 추정 급여가 실제 입금액보다 임계값 이상 크게 계산돼 근거 확인이 필요합니다.
\.


--
-- Data for Name: wage_verification_record_ids; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.wage_verification_record_ids (record_order, record_id, verification_id) FROM stdin;
0	16	1
1	15	1
2	3	1
3	14	1
4	12	1
5	11	1
6	10	1
\.


--
-- Data for Name: wage_verifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.wage_verifications (deductions_known, difference_rate, modified_record_count, threshold_deduction_relaxed, threshold_relative_percent, year_month, actual_deposit_amount, base_estimate, created_at, difference_amount, estimated_total, id, night_minutes, night_premium, overtime_minutes, overtime_premium, threshold_absolute_won, updated_at, user_id, workplace_id, status, resolution_stage, memo) FROM stdin;
f	0.3210	1	t	0.0300	2026-03	550000	764000	2026-03-23 16:26:20.318676	260000	810000	1	0	0	460	46000	24300	2026-03-23 16:26:20.318676	2	1	CHECK_REQUIRED	EMPLOYER_CONFIRMATION_RECOMMENDED	\N
\.


--
-- Data for Name: work_contracts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.work_contracts (base_pay_amount, daily_work_minutes, effective_from, effective_to, monthly_work_minutes, normalized_hourly_wage, created_at, id, updated_at, workplace_id, pay_unit) FROM stdin;
12000.00	480	2026-01-01	\N	10560	12000.00	2026-03-20 16:48:40.430107	1	2026-03-20 16:48:40.430107	1	HOURLY
12000.00	\N	2026-01-01	\N	\N	12000.00	2026-03-26 09:34:27.879096	5	2026-03-26 09:34:27.879096	15	HOURLY
12000.00	\N	2026-03-26	\N	\N	12000.00	2026-03-26 15:21:44.947013	6	2026-03-26 15:21:44.947013	17	HOURLY
12500.00	\N	2026-03-01	\N	\N	12500.00	2026-03-26 17:26:28.93158	7	2026-03-26 17:26:28.93158	16	HOURLY
\.


--
-- Data for Name: work_proof_audit_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.work_proof_audit_logs (after_attachment_count, before_attachment_count, actor_user_id, after_clock_in_at, after_clock_out_at, before_clock_in_at, before_clock_out_at, created_at, id, work_proof_id, after_edit_reason, after_memo, before_edit_reason, before_memo, after_attachment_metadata_json, before_attachment_metadata_json) FROM stdin;
0	0	2	2026-02-09 09:15:00	2026-02-09 18:10:00	2026-02-09 09:00:00	2026-02-09 18:00:00	2026-03-20 16:48:40.4481	1	1	출근 시간 정정	버스 지연으로 앱 입력이 늦었어요.	\N	\N	\N	\N
1	0	2	2026-02-24 09:00:00	2026-02-24 18:20:00	2026-02-24 08:55:00	2026-02-24 18:05:00	2026-03-20 16:48:40.468053	2	2	퇴근 누락 보정	관리자 확인 후 퇴근 시간을 수정했어요.	\N	\N	18313	\N
2	0	2	2026-03-17 09:10:00	2026-03-17 18:10:00	2026-03-17 09:00:00	2026-03-17 17:50:00	2026-03-20 16:48:40.47626	3	3	지문 인식 오류 정정	시스템 오작동으로 수동 보정했어요.	\N	\N	18315	\N
1	0	4	2026-03-24 08:57:00	2026-03-24 18:05:00	2026-03-24 08:57:00	2026-03-24 17:11:00	2026-03-26 09:34:27.993152	14	82	회의 정리 때문에 퇴근 입력이 늦었어요	\N	\N	\N	[{"type":"MEMO","fileName":"운영메모.txt","fileRef":"seed://manager-note"}]	\N
\.


--
-- Data for Name: work_proofs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.work_proofs (attachment_count, clock_in_latitude, clock_in_longitude, clock_out_latitude, clock_out_longitude, clock_out_outside_allowed_radius, work_date, workplace_latitude_snapshot, workplace_longitude_snapshot, clock_in_at, clock_out_at, contract_id, created_at, device_clock_in_at, device_clock_out_at, id, server_clock_in_at, server_clock_out_at, updated_at, user_id, workplace_id, financial_status, clock_in_location_label, clock_out_location_label, workplace_map_label_snapshot, workplace_name_snapshot, edit_reason, memo, workplace_address_snapshot, attachment_metadata_json, recognized_clock_in_at, recognized_clock_out_at) FROM stdin;
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-09	35.2031092	126.8083831	2026-02-09 09:15:00	2026-02-09 18:10:00	1	2026-03-20 16:48:40.44159	2026-02-09 09:00:00	2026-02-09 18:00:00	1	2026-02-09 09:01:00	2026-02-09 18:01:00	2026-03-20 16:48:40.44159	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	출근 시간 정정	버스 지연으로 앱 입력이 늦었어요.	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
1	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-24	35.2031092	126.8083831	2026-02-24 09:00:00	2026-02-24 18:20:00	1	2026-03-20 16:48:40.452535	2026-02-24 08:55:00	2026-02-24 18:05:00	2	2026-02-24 08:56:00	2026-02-24 18:06:00	2026-03-20 16:48:40.452535	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	퇴근 누락 보정	관리자 확인 후 퇴근 시간을 수정했어요.	광주광역시 광산구 하남산단 6번로 107	18312	\N	\N
2	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-17	35.2031092	126.8083831	2026-03-17 09:10:00	2026-03-17 18:10:00	1	2026-03-20 16:48:40.472413	2026-03-17 09:00:00	2026-03-17 17:50:00	3	2026-03-17 09:01:00	2026-03-17 17:51:00	2026-03-20 16:48:40.472413	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	지문 인식 오류 정정	시스템 오작동으로 수동 보정했어요.	광주광역시 광산구 하남산단 6번로 107	18314	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-03	35.2031092	126.8083831	2026-02-03 09:00:00	2026-02-03 18:00:00	1	2026-03-20 16:48:40.480234	2026-02-03 09:00:00	2026-02-03 18:00:00	4	2026-02-03 09:01:00	2026-02-03 18:01:00	2026-03-20 16:48:40.480234	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-05	35.2031092	126.8083831	2026-02-05 09:05:00	2026-02-05 18:05:00	1	2026-03-20 16:48:40.481803	2026-02-05 09:05:00	2026-02-05 18:05:00	5	2026-02-05 09:06:00	2026-02-05 18:06:00	2026-03-20 16:48:40.481803	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-12	35.2031092	126.8083831	2026-02-12 09:00:00	2026-02-12 18:00:00	1	2026-03-20 16:48:40.485636	2026-02-12 09:00:00	2026-02-12 18:00:00	6	2026-02-12 09:01:00	2026-02-12 18:01:00	2026-03-20 16:48:40.485636	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2231092	126.8283831	t	2026-02-16	35.2031092	126.8083831	2026-02-16 09:00:00	2026-02-16 18:00:00	1	2026-03-20 16:48:40.48836	2026-02-16 09:00:00	2026-02-16 18:00:00	7	2026-02-16 09:01:00	2026-02-16 18:01:00	2026-03-20 16:48:40.48836	2	1	NEEDS_REVIEW	광주 SSAFY	광주 SSAFY 후문	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-19	35.2031092	126.8083831	2026-02-19 09:00:00	2026-02-19 17:45:00	1	2026-03-20 16:48:40.490555	2026-02-19 09:00:00	2026-02-19 17:45:00	8	2026-02-19 09:01:00	2026-02-19 17:46:00	2026-03-20 16:48:40.490555	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-02-27	35.2031092	126.8083831	2026-02-27 09:00:00	2026-02-27 18:00:00	1	2026-03-20 16:48:40.493288	2026-02-27 09:00:00	2026-02-27 18:00:00	9	2026-02-27 09:01:00	2026-02-27 18:01:00	2026-03-20 16:48:40.493288	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-03	35.2031092	126.8083831	2026-03-03 09:00:00	2026-03-03 18:00:00	1	2026-03-20 16:48:40.49501	2026-03-03 09:00:00	2026-03-03 18:00:00	10	2026-03-03 09:01:00	2026-03-03 18:01:00	2026-03-20 16:48:40.49501	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-05	35.2031092	126.8083831	2026-03-05 09:00:00	2026-03-05 18:30:00	1	2026-03-20 16:48:40.49717	2026-03-05 09:00:00	2026-03-05 18:30:00	11	2026-03-05 09:01:00	2026-03-05 18:31:00	2026-03-20 16:48:40.49717	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-09	35.2031092	126.8083831	2026-03-09 08:50:00	2026-03-09 18:00:00	1	2026-03-20 16:48:40.498795	2026-03-09 08:50:00	2026-03-09 18:00:00	12	2026-03-09 08:51:00	2026-03-09 18:01:00	2026-03-20 16:48:40.498795	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2231092	126.8283831	t	2026-03-11	35.2031092	126.8083831	2026-03-11 09:00:00	2026-03-11 18:00:00	1	2026-03-20 16:48:40.500437	2026-03-11 09:00:00	2026-03-11 18:00:00	13	2026-03-11 09:01:00	2026-03-11 18:01:00	2026-03-20 16:48:40.500437	2	1	NEEDS_REVIEW	광주 SSAFY	광주 SSAFY 주차장	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-13	35.2031092	126.8083831	2026-03-13 09:00:00	2026-03-13 18:00:00	1	2026-03-20 16:48:40.502067	2026-03-13 09:00:00	2026-03-13 18:00:00	14	2026-03-13 09:01:00	2026-03-13 18:01:00	2026-03-20 16:48:40.502067	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-18	35.2031092	126.8083831	2026-03-18 09:00:00	2026-03-18 18:00:00	1	2026-03-20 16:48:40.504245	2026-03-18 09:00:00	2026-03-18 18:00:00	15	2026-03-18 09:01:00	2026-03-18 18:01:00	2026-03-20 16:48:40.504245	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-19	35.2031092	126.8083831	2026-03-19 09:00:00	2026-03-19 18:00:00	1	2026-03-20 16:48:40.505339	2026-03-19 09:00:00	2026-03-19 18:00:00	16	2026-03-19 09:01:00	2026-03-19 18:01:00	2026-03-20 16:48:40.505339	2	1	REFLECTED	광주 SSAFY	광주 SSAFY	광주 SSAFY	SSAFY	\N	\N	광주광역시 광산구 하남산단 6번로 107	\N	\N	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-25	35.1912314861349	126.81633226228	2026-03-25 09:00:00	2026-03-25 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-25 09:00:00	2026-03-25 18:00:00	92	2026-03-25 09:01:00	2026-03-25 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-25 09:00:00	2026-03-25 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-04	35.1912314861349	126.81633226228	2026-03-04 09:00:00	2026-03-04 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-04 09:00:00	2026-03-04 18:00:00	95	2026-03-04 09:01:00	2026-03-04 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-04 09:00:00	2026-03-04 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-05	35.1912314861349	126.81633226228	2026-03-05 09:00:00	2026-03-05 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-05 09:00:00	2026-03-05 18:00:00	96	2026-03-05 09:01:00	2026-03-05 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-05 09:00:00	2026-03-05 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-06	35.1912314861349	126.81633226228	2026-03-06 09:00:00	2026-03-06 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-06 09:00:00	2026-03-06 18:00:00	97	2026-03-06 09:01:00	2026-03-06 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-06 09:00:00	2026-03-06 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-09	35.1912314861349	126.81633226228	2026-03-09 09:00:00	2026-03-09 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-09 09:00:00	2026-03-09 18:00:00	98	2026-03-09 09:01:00	2026-03-09 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-09 09:00:00	2026-03-09 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-10	35.1912314861349	126.81633226228	2026-03-10 09:00:00	2026-03-10 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-10 09:00:00	2026-03-10 18:00:00	99	2026-03-10 09:01:00	2026-03-10 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-10 09:00:00	2026-03-10 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-17	35.1912314861349	126.81633226228	2026-03-17 09:00:00	2026-03-17 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-17 09:00:00	2026-03-17 18:00:00	91	2026-03-17 09:01:00	2026-03-17 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-17 09:00:00	2026-03-17 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-03	35.1912314861349	126.81633226228	2026-03-03 09:00:00	2026-03-03 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-03 09:00:00	2026-03-03 18:00:00	94	2026-03-03 09:01:00	2026-03-03 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-03 09:00:00	2026-03-03 18:00:00
0	37.501274	127.039585	37.501274	127.039585	f	2026-03-26	37.501274	127.039585	2026-03-26 09:00:00	2026-03-26 18:00:00	5	2026-03-26 09:34:27.891811	2026-03-26 09:00:00	2026-03-26 18:00:00	78	2026-03-26 09:01:00	2026-03-26 18:01:00	2026-03-26 09:34:27.891811	47	15	REFLECTED	정문 게이트	정문 게이트	1층 정문	서울 허브	\N	\N	서울특별시 강남구 테헤란로 212	\N	2026-03-26 09:00:00	2026-03-26 18:00:00
0	35.2031092	126.8083831	35.1889551	126.8245233	f	2026-03-26	35.1912314861349	126.81633226228	2026-03-26 09:00:00	2026-03-26 18:11:55	7	2026-03-26 17:26:28.93158	2026-03-26 09:00:00	2026-03-26 18:11:55	89	2026-03-26 09:01:00	2026-03-27 01:12:02.634678	2026-03-27 01:12:02.638285	51	16	REFLECTED	정문 게이트	제이에이치 주식회사 기본 사업장	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-26 09:00:00	2026-03-26 18:11:55
0	37.501274	127.039585	\N	\N	\N	2026-03-26	37.501274	127.039585	2026-03-26 09:12:00	\N	5	2026-03-26 09:34:27.903394	2026-03-26 09:12:00	\N	79	2026-03-26 09:13:00	\N	2026-03-26 09:34:27.903394	48	15	PENDING	동문 게이트	\N	1층 정문	서울 허브	\N	\N	서울특별시 강남구 테헤란로 212	\N	2026-03-26 09:12:00	\N
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-23	35.1912314861349	126.81633226228	2026-03-23 09:00:00	2026-03-23 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-23 09:00:00	2026-03-23 18:00:00	85	2026-03-23 09:01:00	2026-03-23 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-23 09:00:00	2026-03-23 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-19	35.1912314861349	126.81633226228	2026-03-19 09:00:00	2026-03-19 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-19 09:00:00	2026-03-19 18:00:00	86	2026-03-19 09:01:00	2026-03-19 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-19 09:00:00	2026-03-19 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-20	35.1912314861349	126.81633226228	2026-03-20 09:00:00	2026-03-20 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-20 09:00:00	2026-03-20 18:00:00	87	2026-03-20 09:01:00	2026-03-20 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-20 09:00:00	2026-03-20 18:00:00
1	37.501274	127.039585	37.506274000000005	127.044585	t	2026-03-26	37.501274	127.039585	2026-03-26 08:55:00	2026-03-26 18:07:00	5	2026-03-26 09:34:27.946898	2026-03-26 08:55:00	2026-03-26 18:07:00	80	2026-03-26 08:56:00	2026-03-26 18:08:00	2026-03-26 09:34:27.946898	49	15	NEEDS_REVIEW	주차장 출입구	사업장 외부 도로	1층 정문	서울 허브	GPS 보정 확인 요청	퇴근 위치가 반경 밖으로 기록되어 검토가 필요해요.	서울특별시 강남구 테헤란로 212	[{"type":"PHOTO","fileName":"검토증빙.jpg","fileRef":"seed://review-evidence"}]	2026-03-26 08:55:00	2026-03-26 18:07:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-24	35.1912314861349	126.81633226228	2026-03-24 09:00:00	2026-03-24 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-24 09:00:00	2026-03-24 18:00:00	88	2026-03-24 09:01:00	2026-03-24 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-24 09:00:00	2026-03-24 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-18	35.1912314861349	126.81633226228	2026-03-18 09:00:00	2026-03-18 18:00:00	7	2026-03-26 17:26:28.93158	2026-03-18 09:00:00	2026-03-18 18:00:00	90	2026-03-18 09:01:00	2026-03-18 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-18 09:00:00	2026-03-18 18:00:00
0	37.501274	127.039585	37.501274	127.039585	f	2026-03-25	37.501274	127.039585	2026-03-25 09:03:00	2026-03-25 18:01:00	5	2026-03-26 09:34:27.953855	2026-03-25 09:03:00	2026-03-25 18:01:00	81	2026-03-25 09:04:00	2026-03-25 18:02:00	2026-03-26 09:34:27.953855	47	15	REFLECTED	정문 게이트	정문 게이트	1층 정문	서울 허브	\N	\N	서울특별시 강남구 테헤란로 212	\N	2026-03-25 09:03:00	2026-03-25 18:01:00
1	37.501274	127.039585	37.501274	127.039585	f	2026-03-24	37.501274	127.039585	2026-03-24 08:57:00	2026-03-24 18:05:00	5	2026-03-26 09:34:27.972375	2026-03-24 08:57:00	2026-03-24 17:11:00	82	2026-03-24 08:58:00	2026-03-24 17:12:00	2026-03-26 09:34:27.986463	48	15	REFLECTED	동문 게이트	동문 게이트	1층 정문	서울 허브	회의 정리 때문에 퇴근 입력이 늦었어요	\N	서울특별시 강남구 테헤란로 212	[{"type":"MEMO","fileName":"운영메모.txt","fileRef":"seed://manager-note"}]	2026-03-24 08:57:00	2026-03-24 18:05:00
0	37.501274	127.039585	37.501274	127.039585	f	2026-03-23	37.501274	127.039585	2026-03-23 09:14:00	2026-03-23 18:09:00	5	2026-03-26 09:34:28.016167	2026-03-23 09:14:00	2026-03-23 18:09:00	83	2026-03-23 09:15:00	2026-03-23 18:10:00	2026-03-26 09:34:28.016167	49	15	REFLECTED	주차장 출입구	주차장 출입구	1층 정문	서울 허브	\N	\N	서울특별시 강남구 테헤란로 212	\N	2026-03-23 09:14:00	2026-03-23 18:09:00
0	35.205668	126.8115819	35.205668	126.8115819	f	2026-03-26	35.2053212919427	126.811552010485	2026-03-26 11:02:43	2026-03-26 11:02:48	5	2026-03-26 11:02:44.056527	2026-03-26 11:02:43	2026-03-26 11:02:48	84	2026-03-26 11:02:44.053896	2026-03-26 11:02:48.347921	2026-03-26 11:02:48.354494	1	15	REFLECTED	서울 허브	서울 허브	1층 정문	서울 허브	\N	\N	삼성전자 광주사업장	\N	2026-03-26 11:02:43	2026-03-26 11:02:48
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-11	35.1912314861349	126.81633226228	2026-03-11 09:00:00	2026-03-11 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-11 09:00:00	2026-03-11 18:00:00	100	2026-03-11 09:01:00	2026-03-11 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-11 09:00:00	2026-03-11 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-12	35.1912314861349	126.81633226228	2026-03-12 09:00:00	2026-03-12 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-12 09:00:00	2026-03-12 18:00:00	101	2026-03-12 09:01:00	2026-03-12 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-12 09:00:00	2026-03-12 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-13	35.1912314861349	126.81633226228	2026-03-13 09:00:00	2026-03-13 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-13 09:00:00	2026-03-13 18:00:00	102	2026-03-13 09:01:00	2026-03-13 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-13 09:00:00	2026-03-13 18:00:00
0	35.2031092	126.8083831	35.2031092	126.8083831	f	2026-03-16	35.1912314861349	126.81633226228	2026-03-16 09:00:00	2026-03-16 18:00:00	7	2026-03-26 17:31:14.175629	2026-03-16 09:00:00	2026-03-16 18:00:00	103	2026-03-16 09:01:00	2026-03-16 18:01:00	2026-03-26 17:31:14.175629	51	16	REFLECTED	정문 게이트	정문 게이트	광주 SSAFY	제이에이치 주식회사 기본 사업장	\N	\N	원당산새싹작은도서관	\N	2026-03-16 09:00:00	2026-03-16 18:00:00
\.


--
-- Data for Name: worker_registration_codes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.worker_registration_codes (id, created_at, updated_at, code_hash, company_id, encrypted_code, issued_by_account_id, revoked_at, workplace_id) FROM stdin;
1	2026-03-25 17:15:59.205078	2026-03-25 17:15:59.205078	e76574cc66afd15d89b31331ff7e8101ecfe2b9451bcc48a14a81639c912b63e	8	IVjVzIYvzKtfGmxUiriLCltMQUGdpYcRhM37M/p/g0hZCMzqBqCt3UkW81k=	4	\N	12
2	2026-03-26 09:43:47.535152	2026-03-26 09:43:47.535152	11af2bb634af9d357e4dc28a47d863069cd26a43b2f3bc5d92ae43672806b02a	11	qG9rfvoBmXqZNGqXSmYF/8UsBeiIaDRSJ9+j8ygyFJ/ifOrZ9m9a0oRQovo=	4	\N	15
3	2026-03-26 15:28:16.294161	2026-03-26 15:28:16.294161	a28b79eaf3f4f27cae9a581421e00438ec5ae15f00a8952e29b65e05eb229ab9	12	C/etboBKGKPn7QnbQLcPZ6WqSMKLSD7b9rrLriZvrISq1Jz5bAJCbNPl4dU=	52	\N	17
\.


--
-- Data for Name: workplaces; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.workplaces (allowed_radius_meters, latitude, longitude, company_id, created_at, id, settings_effective_from, settings_updated_by_account_id, updated_at, user_id, map_label, name, address) FROM stdin;
1000	35.2031092	126.8083831	\N	2026-03-20 16:48:40.420234	1	\N	\N	2026-03-20 16:48:40.420234	2	광주 SSAFY	SSAFY	광주광역시 광산구 하남산단 6번로 107
1000	35.2031092	126.8083831	\N	2026-03-20 16:54:03.894842	2	\N	\N	2026-03-20 16:54:03.894842	1	광주 SSAFY	SSAFY (임시)	광주광역시 광산구 하남산단 6번로 107
1000	35.2031092	126.8083831	\N	2026-03-23 09:26:29.229363	6	\N	\N	2026-03-23 09:26:29.229363	17	광주 SSAFY	SSAFY (임시)	광주광역시 광산구 하남산단 6번로 107
1000	35.2031092	126.8083831	\N	2026-03-23 10:55:05.904143	7	\N	\N	2026-03-23 10:55:05.904143	18	광주 SSAFY	SSAFY (임시)	광주광역시 광산구 하남산단 6번로 107
1000	35.2053212919427	126.811552010485	11	2026-03-26 09:34:27.485048	15	2026-03-26 09:45:50.169391	4	2026-03-26 09:45:50.170431	4	1층 정문	서울 허브	삼성전자 광주사업장
1000	35.1912314861349	126.81633226228	12	2026-03-26 15:21:44.936721	17	2026-03-26 15:23:44.329076	52	2026-03-26 15:23:44.330242	3	\N	제이에이치 주식회사 기본 사업장	원당산새싹작은도서관
1000	35.2031092	126.8083831	\N	2026-03-26 15:35:00.5627	18	\N	\N	2026-03-26 15:35:00.5627	53	광주 SSAFY	SSAFY (임시)	광주광역시 광산구 하남산단 6번로 107
1000	35.2031092	126.8083831	\N	2026-03-26 16:17:51.329162	19	\N	\N	2026-03-26 16:17:51.329162	54	광주 SSAFY	SSAFY (임시)	광주광역시 광산구 하남산단 6번로 107
1000	35.1912314861349	126.81633226228	12	2026-03-26 15:20:03.845384	16	\N	\N	2026-03-26 15:20:03.845384	51	광주 SSAFY	제이에이치 주식회사 기본 사업장	원당산새싹작은도서관
\.


--
-- Data for Name: advance_requests; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.advance_requests (id, user_id, workplace_id, contract_id, month_start, idempotency_key, requested_amount_krw, approved_amount_krw, fee_amount_krw, status, requested_at, repayment_due_date, snapshot_available_amount_krw, snapshot_max_cap_krw, snapshot_policy_rate, snapshot_repayment_tier, snapshot_reflected_work_days, snapshot_reflected_work_minutes, snapshot_verified_minutes, snapshot_pending_minutes, snapshot_needs_review_record_count, snapshot_next_tier_remaining_minutes, snapshot_estimated_fee_krw, snapshot_estimated_repayment_date, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: async_job_attempts; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.async_job_attempts (id, job_id, attempt_no, status, worker_name, started_at, finished_at, error_code, error_message) FROM stdin;
\.


--
-- Data for Name: async_jobs; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.async_jobs (id, job_type, queue_name, resource_type, resource_id, status, priority, dedupe_key, concurrency_key, payload_json, attempts, max_attempts, run_after, locked_at, locked_by, last_error_code, last_error_message, completed_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: claim_preparation_checklist_items; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.claim_preparation_checklist_items (id, preparation_id, sort_order, item_code, item_text, is_required) FROM stdin;
\.


--
-- Data for Name: claim_preparation_document_links; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.claim_preparation_document_links (preparation_id, document_id, sort_order) FROM stdin;
\.


--
-- Data for Name: claim_preparation_route_snapshots; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.claim_preparation_route_snapshots (id, preparation_id, sort_order, channel, title, description, contact, link_url) FROM stdin;
\.


--
-- Data for Name: claim_preparations; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.claim_preparations (id, user_id, wage_verification_id, claim_kit_document_id, idempotency_key, locale, tone, status, summary_text, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: claim_routes; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.claim_routes (id, locale, channel, title, description, contact, link_url, sort_order, is_active, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: documents; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.documents (id, request_id, requested_by_user_id, document_type, document_status, document_format, title, idempotency_key, wage_verification_id, transfer_id, workplace_id, month_start, summary_payload_json, related_links_json, mime_type, file_name, file_size_bytes, storage_provider, storage_object_key, file_sha256, error_code, error_message, queued_at, started_at, ready_at, failed_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: remittance_recipients; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.remittance_recipients (id, user_id, name, alias, relationship, wallet_address, photo_url, is_favorite, cooldown_until, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: remittance_transfer_events; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.remittance_transfer_events (id, transfer_id, event_type, message, tx_hash, payload_json, created_at) FROM stdin;
\.


--
-- Data for Name: remittance_transfers; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.remittance_transfers (id, request_id, user_id, recipient_id, safepay_check_id, idempotency_key, token_symbol, token_decimals, amount, memo, status, tx_hash, failure_reason_code, failure_reason_message, submitted_at, confirmed_at, failed_at, blocked_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: safepay_transfer_check_reasons; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.safepay_transfer_check_reasons (id, safepay_check_id, sort_order, reason_code) FROM stdin;
\.


--
-- Data for Name: safepay_transfer_checks; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.safepay_transfer_checks (id, user_id, recipient_id, token_symbol, amount, decision, user_message, cooldown_until, requires_additional_confirm, checked_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.users (id, email, password_hash, name, role, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: vault_accounts; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.vault_accounts (id, user_id, token_symbol, stored_amount, available_to_store_amount, available_to_transfer_amount, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: vault_ledger_entries; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.vault_ledger_entries (id, account_id, entry_type, amount, release_target, resulting_stored_amount, resulting_available_to_store_amount, resulting_available_to_transfer_amount, interest_daily, interest_monthly, interest_apr, simulated_at, created_at) FROM stdin;
\.


--
-- Data for Name: wage_deposits; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.wage_deposits (id, user_id, month_start, deposit_date, actual_deposit_amount_krw, deductions_known, note, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: wage_verification_cause_snapshots; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.wage_verification_cause_snapshots (id, verification_id, sort_order, cause_code, cause_title, cause_detail) FROM stdin;
\.


--
-- Data for Name: wage_verification_record_links; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.wage_verification_record_links (verification_id, sort_order, work_proof_record_id) FROM stdin;
\.


--
-- Data for Name: wage_verifications; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.wage_verifications (id, user_id, workplace_id, contract_id, month_start, pay_unit, base_pay_amount_krw, daily_work_minutes, monthly_work_minutes, normalized_hourly_wage_krw, work_day_count, verified_work_minutes, overtime_minutes, night_minutes, modified_record_count, excluded_pending_record_count, actual_deposit_amount_krw, deductions_known, submitted_by, memo, status, resolution_stage, base_estimate_krw, overtime_premium_krw, night_premium_krw, estimated_total_krw, difference_amount_krw, difference_rate, threshold_absolute_won, threshold_relative_percent, threshold_deduction_relaxed, rule_version, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: work_contracts; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.work_contracts (id, workplace_id, pay_unit, base_pay_amount_krw, daily_work_minutes, monthly_work_minutes, normalized_hourly_wage_krw, effective_from, effective_to, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: work_proof_attachments; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.work_proof_attachments (id, user_id, kind, file_name, content_type, file_size_bytes, storage_provider, storage_object_key, file_sha256, uploaded_at) FROM stdin;
\.


--
-- Data for Name: work_proof_modification_attachments; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.work_proof_modification_attachments (modification_id, attachment_id) FROM stdin;
\.


--
-- Data for Name: work_proof_modifications; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.work_proof_modifications (id, record_id, modified_by_user_id, reviewed_by_user_id, review_status, modification_kind, reason_code, reason_memo, before_record_status, before_reflection_status, before_check_in_device_at, before_check_out_device_at, after_record_status, after_reflection_status, after_check_in_device_at, after_check_out_device_at, reviewed_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: work_proof_records; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.work_proof_records (id, user_id, workplace_id, contract_id, source_type, record_status, reflection_status, work_date, worked_minutes, modification_count, check_in_device_at, check_in_server_at, check_in_latitude, check_in_longitude, check_in_location_label, check_out_device_at, check_out_server_at, check_out_latitude, check_out_longitude, check_out_location_label, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: workplaces; Type: TABLE DATA; Schema: test; Owner: -
--

COPY test.workplaces (id, user_id, name, address, map_label, latitude, longitude, created_at, updated_at) FROM stdin;
\.


--
-- Name: advance_policies_advance_policy_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.advance_policies_advance_policy_id_seq', 1, true);


--
-- Name: advance_requests_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.advance_requests_id_seq', 4, true);


--
-- Name: claim_preparations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.claim_preparations_id_seq', 1, false);


--
-- Name: companies_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.companies_id_seq', 12, true);


--
-- Name: correction_decision_audits_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.correction_decision_audits_id_seq', 22, true);


--
-- Name: correction_requests_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.correction_requests_id_seq', 33, true);


--
-- Name: document_generation_requests_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.document_generation_requests_id_seq', 4, true);


--
-- Name: employer_invitation_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.employer_invitation_tokens_id_seq', 1, false);


--
-- Name: employer_profiles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.employer_profiles_id_seq', 12, true);


--
-- Name: employer_signup_codes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.employer_signup_codes_id_seq', 12, true);


--
-- Name: employment_memberships_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.employment_memberships_id_seq', 47, true);


--
-- Name: jobs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.jobs_id_seq', 22, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 54, true);


--
-- Name: vault_positions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vault_positions_id_seq', 2, true);


--
-- Name: vault_yield_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vault_yield_logs_id_seq', 28, true);


--
-- Name: wage_deposits_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.wage_deposits_id_seq', 61, true);


--
-- Name: wage_verifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.wage_verifications_id_seq', 1, true);


--
-- Name: work_contracts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.work_contracts_id_seq', 7, true);


--
-- Name: work_proof_audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.work_proof_audit_logs_id_seq', 14, true);


--
-- Name: work_proofs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.work_proofs_id_seq', 106, true);


--
-- Name: worker_registration_codes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.worker_registration_codes_id_seq', 3, true);


--
-- Name: workplaces_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.workplaces_id_seq', 19, true);


--
-- Name: advance_requests_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.advance_requests_id_seq', 1, false);


--
-- Name: async_job_attempts_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.async_job_attempts_id_seq', 1, false);


--
-- Name: async_jobs_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.async_jobs_id_seq', 1, false);


--
-- Name: claim_preparation_checklist_items_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.claim_preparation_checklist_items_id_seq', 1, false);


--
-- Name: claim_preparation_route_snapshots_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.claim_preparation_route_snapshots_id_seq', 1, false);


--
-- Name: claim_preparations_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.claim_preparations_id_seq', 1, false);


--
-- Name: claim_routes_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.claim_routes_id_seq', 1, false);


--
-- Name: documents_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.documents_id_seq', 1, false);


--
-- Name: remittance_recipients_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.remittance_recipients_id_seq', 1, false);


--
-- Name: remittance_transfer_events_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.remittance_transfer_events_id_seq', 1, false);


--
-- Name: remittance_transfers_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.remittance_transfers_id_seq', 1, false);


--
-- Name: safepay_transfer_check_reasons_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.safepay_transfer_check_reasons_id_seq', 1, false);


--
-- Name: safepay_transfer_checks_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.safepay_transfer_checks_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.users_id_seq', 1, false);


--
-- Name: vault_accounts_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.vault_accounts_id_seq', 1, false);


--
-- Name: vault_ledger_entries_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.vault_ledger_entries_id_seq', 1, false);


--
-- Name: wage_deposits_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.wage_deposits_id_seq', 1, false);


--
-- Name: wage_verification_cause_snapshots_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.wage_verification_cause_snapshots_id_seq', 1, false);


--
-- Name: wage_verifications_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.wage_verifications_id_seq', 1, false);


--
-- Name: work_contracts_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.work_contracts_id_seq', 1, false);


--
-- Name: work_proof_attachments_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.work_proof_attachments_id_seq', 1, false);


--
-- Name: work_proof_modifications_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.work_proof_modifications_id_seq', 1, false);


--
-- Name: work_proof_records_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.work_proof_records_id_seq', 1, false);


--
-- Name: workplaces_id_seq; Type: SEQUENCE SET; Schema: test; Owner: -
--

SELECT pg_catalog.setval('test.workplaces_id_seq', 1, false);


--
-- Data for Name: BLOBS; Type: BLOBS; Schema: -; Owner: -
--

BEGIN;

SELECT pg_catalog.lo_open('18312', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226174746163686d656e7473223a5b7b226e616d65223a22636c6f636b2d636f7272656374696f6e2e6a7067227d5d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('18313', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226174746163686d656e7473223a5b7b226e616d65223a22636c6f636b2d636f7272656374696f6e2e6a7067227d5d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('18314', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226174746163686d656e7473223a5b7b226e616d65223a22636c6f636b2d636f7272656374696f6e2e6a7067227d5d7d');
SELECT pg_catalog.lo_close(0);

SELECT pg_catalog.lo_open('18315', 131072);
SELECT pg_catalog.lowrite(0, '\x7b226174746163686d656e7473223a5b7b226e616d65223a22636c6f636b2d636f7272656374696f6e2e6a7067227d5d7d');
SELECT pg_catalog.lo_close(0);

COMMIT;

--
-- Name: advance_payouts advance_payouts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_payouts
    ADD CONSTRAINT advance_payouts_pkey PRIMARY KEY (advance_payout_id);


--
-- Name: advance_policies advance_policies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_policies
    ADD CONSTRAINT advance_policies_pkey PRIMARY KEY (advance_policy_id);


--
-- Name: advance_requests advance_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_requests
    ADD CONSTRAINT advance_requests_pkey PRIMARY KEY (id);


--
-- Name: claim_preparations claim_preparations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_preparations
    ADD CONSTRAINT claim_preparations_pkey PRIMARY KEY (id);


--
-- Name: companies companies_company_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_company_code_key UNIQUE (company_code);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: correction_decision_audits correction_decision_audits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.correction_decision_audits
    ADD CONSTRAINT correction_decision_audits_pkey PRIMARY KEY (id);


--
-- Name: correction_requests correction_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.correction_requests
    ADD CONSTRAINT correction_requests_pkey PRIMARY KEY (id);


--
-- Name: document_generation_requests document_generation_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_generation_requests
    ADD CONSTRAINT document_generation_requests_pkey PRIMARY KEY (id);


--
-- Name: document_generation_requests document_generation_requests_request_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_generation_requests
    ADD CONSTRAINT document_generation_requests_request_id_key UNIQUE (request_id);


--
-- Name: employer_invitation_tokens employer_invitation_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_invitation_tokens
    ADD CONSTRAINT employer_invitation_tokens_pkey PRIMARY KEY (id);


--
-- Name: employer_profiles employer_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_profiles
    ADD CONSTRAINT employer_profiles_pkey PRIMARY KEY (id);


--
-- Name: employer_signup_codes employer_signup_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_signup_codes
    ADD CONSTRAINT employer_signup_codes_pkey PRIMARY KEY (id);


--
-- Name: employment_memberships employment_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employment_memberships
    ADD CONSTRAINT employment_memberships_pkey PRIMARY KEY (id);


--
-- Name: jobs jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs
    ADD CONSTRAINT jobs_pkey PRIMARY KEY (id);


--
-- Name: recipients recipients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipients
    ADD CONSTRAINT recipients_pkey PRIMARY KEY (recipient_id);


--
-- Name: transfers transfers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfers
    ADD CONSTRAINT transfers_pkey PRIMARY KEY (transfer_id);


--
-- Name: advance_payouts uk_advance_payouts_advance_request; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_payouts
    ADD CONSTRAINT uk_advance_payouts_advance_request UNIQUE (advance_request_id);


--
-- Name: advance_payouts uk_advance_payouts_tx_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_payouts
    ADD CONSTRAINT uk_advance_payouts_tx_hash UNIQUE (tx_hash);


--
-- Name: advance_payouts uk_advance_payouts_user_idempotency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_payouts
    ADD CONSTRAINT uk_advance_payouts_user_idempotency UNIQUE (user_id, idempotency_key);


--
-- Name: advance_requests uk_advance_requests_user_idempotency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_requests
    ADD CONSTRAINT uk_advance_requests_user_idempotency UNIQUE (user_id, idempotency_key);


--
-- Name: document_generation_requests uk_document_generation_requests_user_type_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_generation_requests
    ADD CONSTRAINT uk_document_generation_requests_user_type_key UNIQUE (user_id, document_type, idempotency_key);


--
-- Name: employer_invitation_tokens uk_employer_invitation_tokens_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_invitation_tokens
    ADD CONSTRAINT uk_employer_invitation_tokens_token_hash UNIQUE (token_hash);


--
-- Name: employer_profiles uk_employer_profiles_account_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_profiles
    ADD CONSTRAINT uk_employer_profiles_account_id UNIQUE (account_id);


--
-- Name: employer_signup_codes uk_employer_signup_codes_code_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employer_signup_codes
    ADD CONSTRAINT uk_employer_signup_codes_code_hash UNIQUE (code_hash);


--
-- Name: jobs uk_jobs_active_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs
    ADD CONSTRAINT uk_jobs_active_key UNIQUE (active_key);


--
-- Name: recipients uk_recipients_user_wallet; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipients
    ADD CONSTRAINT uk_recipients_user_wallet UNIQUE (user_id, wallet_address);


--
-- Name: transfers uk_transfers_tx_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfers
    ADD CONSTRAINT uk_transfers_tx_hash UNIQUE (tx_hash);


--
-- Name: transfers uk_transfers_user_idempotency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfers
    ADD CONSTRAINT uk_transfers_user_idempotency UNIQUE (user_id, idempotency_key);


--
-- Name: vault_positions uk_vault_positions_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_positions
    ADD CONSTRAINT uk_vault_positions_user UNIQUE (user_id);


--
-- Name: vault_transactions uk_vault_transactions_tx_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_transactions
    ADD CONSTRAINT uk_vault_transactions_tx_hash UNIQUE (tx_hash);


--
-- Name: vault_transactions uk_vault_transactions_user_idempotency; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_transactions
    ADD CONSTRAINT uk_vault_transactions_user_idempotency UNIQUE (user_id, idempotency_key);


--
-- Name: worker_registration_codes uk_worker_registration_codes_code_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_registration_codes
    ADD CONSTRAINT uk_worker_registration_codes_code_hash UNIQUE (code_hash);


--
-- Name: user_wallets user_wallets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_wallets
    ADD CONSTRAINT user_wallets_pkey PRIMARY KEY (user_id);


--
-- Name: user_wallets user_wallets_wallet_address_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_wallets
    ADD CONSTRAINT user_wallets_wallet_address_key UNIQUE (wallet_address);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_phone_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_number_key UNIQUE (phone_number);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vault_positions vault_positions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_positions
    ADD CONSTRAINT vault_positions_pkey PRIMARY KEY (id);


--
-- Name: vault_transactions vault_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_transactions
    ADD CONSTRAINT vault_transactions_pkey PRIMARY KEY (vault_transaction_id);


--
-- Name: vault_yield_logs vault_yield_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_yield_logs
    ADD CONSTRAINT vault_yield_logs_pkey PRIMARY KEY (id);


--
-- Name: wage_deposits wage_deposits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_deposits
    ADD CONSTRAINT wage_deposits_pkey PRIMARY KEY (id);


--
-- Name: wage_verification_possible_causes wage_verification_possible_causes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verification_possible_causes
    ADD CONSTRAINT wage_verification_possible_causes_pkey PRIMARY KEY (cause_order, verification_id);


--
-- Name: wage_verification_record_ids wage_verification_record_ids_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verification_record_ids
    ADD CONSTRAINT wage_verification_record_ids_pkey PRIMARY KEY (record_order, verification_id);


--
-- Name: wage_verifications wage_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verifications
    ADD CONSTRAINT wage_verifications_pkey PRIMARY KEY (id);


--
-- Name: work_contracts work_contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_contracts
    ADD CONSTRAINT work_contracts_pkey PRIMARY KEY (id);


--
-- Name: work_proof_audit_logs work_proof_audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proof_audit_logs
    ADD CONSTRAINT work_proof_audit_logs_pkey PRIMARY KEY (id);


--
-- Name: work_proofs work_proofs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proofs
    ADD CONSTRAINT work_proofs_pkey PRIMARY KEY (id);


--
-- Name: worker_registration_codes worker_registration_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.worker_registration_codes
    ADD CONSTRAINT worker_registration_codes_pkey PRIMARY KEY (id);


--
-- Name: workplaces workplaces_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workplaces
    ADD CONSTRAINT workplaces_pkey PRIMARY KEY (id);


--
-- Name: advance_requests advance_requests_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.advance_requests
    ADD CONSTRAINT advance_requests_pkey PRIMARY KEY (id);


--
-- Name: async_job_attempts async_job_attempts_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.async_job_attempts
    ADD CONSTRAINT async_job_attempts_pkey PRIMARY KEY (id);


--
-- Name: async_jobs async_jobs_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.async_jobs
    ADD CONSTRAINT async_jobs_pkey PRIMARY KEY (id);


--
-- Name: claim_preparation_checklist_items claim_preparation_checklist_items_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_checklist_items
    ADD CONSTRAINT claim_preparation_checklist_items_pkey PRIMARY KEY (id);


--
-- Name: claim_preparation_document_links claim_preparation_document_links_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_document_links
    ADD CONSTRAINT claim_preparation_document_links_pkey PRIMARY KEY (preparation_id, document_id);


--
-- Name: claim_preparation_route_snapshots claim_preparation_route_snapshots_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_route_snapshots
    ADD CONSTRAINT claim_preparation_route_snapshots_pkey PRIMARY KEY (id);


--
-- Name: claim_preparations claim_preparations_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparations
    ADD CONSTRAINT claim_preparations_pkey PRIMARY KEY (id);


--
-- Name: claim_routes claim_routes_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_routes
    ADD CONSTRAINT claim_routes_pkey PRIMARY KEY (id);


--
-- Name: documents documents_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.documents
    ADD CONSTRAINT documents_pkey PRIMARY KEY (id);


--
-- Name: work_contracts ex_work_contracts_no_overlap; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_contracts
    ADD CONSTRAINT ex_work_contracts_no_overlap EXCLUDE USING gist (workplace_id WITH =, daterange(effective_from, COALESCE((effective_to + 1), 'infinity'::date), '[)'::text) WITH &&);


--
-- Name: remittance_recipients remittance_recipients_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_recipients
    ADD CONSTRAINT remittance_recipients_pkey PRIMARY KEY (id);


--
-- Name: remittance_transfer_events remittance_transfer_events_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_transfer_events
    ADD CONSTRAINT remittance_transfer_events_pkey PRIMARY KEY (id);


--
-- Name: remittance_transfers remittance_transfers_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_transfers
    ADD CONSTRAINT remittance_transfers_pkey PRIMARY KEY (id);


--
-- Name: safepay_transfer_check_reasons safepay_transfer_check_reasons_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.safepay_transfer_check_reasons
    ADD CONSTRAINT safepay_transfer_check_reasons_pkey PRIMARY KEY (id);


--
-- Name: safepay_transfer_checks safepay_transfer_checks_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.safepay_transfer_checks
    ADD CONSTRAINT safepay_transfer_checks_pkey PRIMARY KEY (id);


--
-- Name: async_job_attempts uk_async_job_attempts_job_attempt; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.async_job_attempts
    ADD CONSTRAINT uk_async_job_attempts_job_attempt UNIQUE (job_id, attempt_no);


--
-- Name: claim_preparation_checklist_items uk_claim_preparation_checklist_sort; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_checklist_items
    ADD CONSTRAINT uk_claim_preparation_checklist_sort UNIQUE (preparation_id, sort_order);


--
-- Name: claim_preparation_document_links uk_claim_preparation_document_links_sort; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_document_links
    ADD CONSTRAINT uk_claim_preparation_document_links_sort UNIQUE (preparation_id, sort_order);


--
-- Name: claim_preparation_route_snapshots uk_claim_preparation_routes_sort; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_route_snapshots
    ADD CONSTRAINT uk_claim_preparation_routes_sort UNIQUE (preparation_id, sort_order);


--
-- Name: safepay_transfer_check_reasons uk_safepay_transfer_check_reasons_sort; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.safepay_transfer_check_reasons
    ADD CONSTRAINT uk_safepay_transfer_check_reasons_sort UNIQUE (safepay_check_id, sort_order);


--
-- Name: wage_verification_cause_snapshots uk_wage_verification_cause_snapshots_order; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_cause_snapshots
    ADD CONSTRAINT uk_wage_verification_cause_snapshots_order UNIQUE (verification_id, sort_order);


--
-- Name: wage_verification_record_links uk_wage_verification_record_links_record; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_record_links
    ADD CONSTRAINT uk_wage_verification_record_links_record UNIQUE (verification_id, work_proof_record_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vault_accounts vault_accounts_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.vault_accounts
    ADD CONSTRAINT vault_accounts_pkey PRIMARY KEY (id);


--
-- Name: vault_ledger_entries vault_ledger_entries_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.vault_ledger_entries
    ADD CONSTRAINT vault_ledger_entries_pkey PRIMARY KEY (id);


--
-- Name: wage_deposits wage_deposits_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_deposits
    ADD CONSTRAINT wage_deposits_pkey PRIMARY KEY (id);


--
-- Name: wage_verification_cause_snapshots wage_verification_cause_snapshots_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_cause_snapshots
    ADD CONSTRAINT wage_verification_cause_snapshots_pkey PRIMARY KEY (id);


--
-- Name: wage_verification_record_links wage_verification_record_links_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_record_links
    ADD CONSTRAINT wage_verification_record_links_pkey PRIMARY KEY (verification_id, sort_order);


--
-- Name: wage_verifications wage_verifications_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verifications
    ADD CONSTRAINT wage_verifications_pkey PRIMARY KEY (id);


--
-- Name: work_contracts work_contracts_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_contracts
    ADD CONSTRAINT work_contracts_pkey PRIMARY KEY (id);


--
-- Name: work_proof_attachments work_proof_attachments_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_attachments
    ADD CONSTRAINT work_proof_attachments_pkey PRIMARY KEY (id);


--
-- Name: work_proof_modification_attachments work_proof_modification_attachments_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modification_attachments
    ADD CONSTRAINT work_proof_modification_attachments_pkey PRIMARY KEY (modification_id, attachment_id);


--
-- Name: work_proof_modifications work_proof_modifications_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modifications
    ADD CONSTRAINT work_proof_modifications_pkey PRIMARY KEY (id);


--
-- Name: work_proof_records work_proof_records_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_records
    ADD CONSTRAINT work_proof_records_pkey PRIMARY KEY (id);


--
-- Name: workplaces workplaces_pkey; Type: CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.workplaces
    ADD CONSTRAINT workplaces_pkey PRIMARY KEY (id);


--
-- Name: idx_advance_payouts_status_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_advance_payouts_status_updated_at ON public.advance_payouts USING btree (status, updated_at DESC, advance_payout_id DESC);


--
-- Name: idx_advance_payouts_user_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_advance_payouts_user_created_at ON public.advance_payouts USING btree (user_id, created_at DESC, advance_payout_id DESC);


--
-- Name: idx_advance_payouts_user_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_advance_payouts_user_status ON public.advance_payouts USING btree (user_id, status);


--
-- Name: idx_advance_policies_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_advance_policies_enabled ON public.advance_policies USING btree (enabled, advance_policy_id DESC);


--
-- Name: idx_jobs_reference_kind_id_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobs_reference_kind_id_type ON public.jobs USING btree (reference_kind, reference_id, job_type);


--
-- Name: idx_jobs_status_run_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jobs_status_run_at ON public.jobs USING btree (status, run_at, id);


--
-- Name: idx_recipients_target_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipients_target_user_id ON public.recipients USING btree (target_user_id);


--
-- Name: idx_recipients_user_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recipients_user_updated_at ON public.recipients USING btree (user_id, updated_at DESC, recipient_id DESC);


--
-- Name: idx_transfers_status_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transfers_status_updated_at ON public.transfers USING btree (status, updated_at DESC, transfer_id DESC);


--
-- Name: idx_transfers_user_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transfers_user_created_at ON public.transfers USING btree (user_id, created_at DESC, transfer_id DESC);


--
-- Name: idx_transfers_user_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transfers_user_status ON public.transfers USING btree (user_id, status);


--
-- Name: idx_user_wallets_funding_status_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_wallets_funding_status_updated_at ON public.user_wallets USING btree (funding_status, updated_at DESC, user_id DESC);


--
-- Name: idx_vault_positions_status_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vault_positions_status_updated_at ON public.vault_positions USING btree (status, updated_at DESC, id DESC);


--
-- Name: idx_vault_transactions_status_updated_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vault_transactions_status_updated_at ON public.vault_transactions USING btree (status, updated_at DESC, vault_transaction_id DESC);


--
-- Name: idx_vault_transactions_user_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vault_transactions_user_created_at ON public.vault_transactions USING btree (user_id, created_at DESC, vault_transaction_id DESC);


--
-- Name: idx_vault_transactions_user_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vault_transactions_user_status ON public.vault_transactions USING btree (user_id, status);


--
-- Name: idx_advance_requests_user_month_start; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_advance_requests_user_month_start ON test.advance_requests USING btree (user_id, month_start, requested_at DESC, id DESC);


--
-- Name: idx_async_job_attempts_job_started_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_async_job_attempts_job_started_at ON test.async_job_attempts USING btree (job_id, started_at DESC, id DESC);


--
-- Name: idx_async_jobs_concurrency_key; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_async_jobs_concurrency_key ON test.async_jobs USING btree (concurrency_key) WHERE (concurrency_key IS NOT NULL);


--
-- Name: idx_async_jobs_queue_status_run_after; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_async_jobs_queue_status_run_after ON test.async_jobs USING btree (queue_name, status, run_after, priority, id);


--
-- Name: idx_async_jobs_resource_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_async_jobs_resource_created_at ON test.async_jobs USING btree (resource_type, resource_id, created_at DESC, id DESC);


--
-- Name: idx_async_jobs_status_run_after; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_async_jobs_status_run_after ON test.async_jobs USING btree (status, run_after, priority, id);


--
-- Name: idx_claim_preparations_user_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_claim_preparations_user_created_at ON test.claim_preparations USING btree (user_id, created_at DESC, id DESC);


--
-- Name: idx_claim_preparations_verification_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_claim_preparations_verification_created_at ON test.claim_preparations USING btree (wage_verification_id, created_at DESC, id DESC);


--
-- Name: idx_claim_routes_locale_active_sort; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_claim_routes_locale_active_sort ON test.claim_routes USING btree (locale, is_active, sort_order, id);


--
-- Name: idx_documents_user_month_start; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_documents_user_month_start ON test.documents USING btree (requested_by_user_id, month_start, updated_at DESC, id DESC);


--
-- Name: idx_documents_user_status_updated_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_documents_user_status_updated_at ON test.documents USING btree (requested_by_user_id, document_status, updated_at DESC, id DESC);


--
-- Name: idx_documents_user_type_updated_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_documents_user_type_updated_at ON test.documents USING btree (requested_by_user_id, document_type, updated_at DESC, id DESC);


--
-- Name: idx_documents_wage_verification_id; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_documents_wage_verification_id ON test.documents USING btree (wage_verification_id, created_at DESC, id DESC) WHERE (wage_verification_id IS NOT NULL);


--
-- Name: idx_remittance_recipients_user_favorite_created; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_remittance_recipients_user_favorite_created ON test.remittance_recipients USING btree (user_id, is_favorite DESC, created_at DESC, id DESC);


--
-- Name: idx_remittance_transfer_events_transfer_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_remittance_transfer_events_transfer_created_at ON test.remittance_transfer_events USING btree (transfer_id, created_at DESC, id DESC);


--
-- Name: idx_remittance_transfers_user_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_remittance_transfers_user_created_at ON test.remittance_transfers USING btree (user_id, created_at DESC, id DESC);


--
-- Name: idx_remittance_transfers_user_status_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_remittance_transfers_user_status_created_at ON test.remittance_transfers USING btree (user_id, status, created_at DESC, id DESC);


--
-- Name: idx_safepay_transfer_checks_user_checked_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_safepay_transfer_checks_user_checked_at ON test.safepay_transfer_checks USING btree (user_id, checked_at DESC, id DESC);


--
-- Name: idx_vault_ledger_entries_account_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_vault_ledger_entries_account_created_at ON test.vault_ledger_entries USING btree (account_id, created_at DESC, id DESC);


--
-- Name: idx_wage_deposits_user_month_start; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_wage_deposits_user_month_start ON test.wage_deposits USING btree (user_id, month_start, deposit_date DESC, created_at DESC, id DESC);


--
-- Name: idx_wage_verification_record_links_record_id; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_wage_verification_record_links_record_id ON test.wage_verification_record_links USING btree (work_proof_record_id);


--
-- Name: idx_wage_verifications_user_month_start_workplace; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_wage_verifications_user_month_start_workplace ON test.wage_verifications USING btree (user_id, month_start, workplace_id, created_at DESC, id DESC);


--
-- Name: idx_work_contracts_workplace_effective_from; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_work_contracts_workplace_effective_from ON test.work_contracts USING btree (workplace_id, effective_from DESC, id DESC);


--
-- Name: idx_work_proof_attachments_user_uploaded_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_work_proof_attachments_user_uploaded_at ON test.work_proof_attachments USING btree (user_id, uploaded_at DESC, id DESC);


--
-- Name: idx_work_proof_modification_attachments_attachment_id; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_work_proof_modification_attachments_attachment_id ON test.work_proof_modification_attachments USING btree (attachment_id);


--
-- Name: idx_work_proof_modifications_record_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_work_proof_modifications_record_created_at ON test.work_proof_modifications USING btree (record_id, created_at DESC, id DESC);


--
-- Name: idx_work_proof_records_user_work_date; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_work_proof_records_user_work_date ON test.work_proof_records USING btree (user_id, work_date DESC, created_at DESC, id DESC);


--
-- Name: idx_work_proof_records_user_workplace_work_date; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_work_proof_records_user_workplace_work_date ON test.work_proof_records USING btree (user_id, workplace_id, work_date DESC, created_at DESC, id DESC);


--
-- Name: idx_workplaces_user_created_at; Type: INDEX; Schema: test; Owner: -
--

CREATE INDEX idx_workplaces_user_created_at ON test.workplaces USING btree (user_id, created_at DESC, id DESC);


--
-- Name: uk_advance_requests_active_month; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_advance_requests_active_month ON test.advance_requests USING btree (user_id, workplace_id, month_start) WHERE ((status)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'APPROVED'::character varying, 'NEEDS_REVIEW'::character varying])::text[]));


--
-- Name: uk_advance_requests_user_idempotency; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_advance_requests_user_idempotency ON test.advance_requests USING btree (user_id, idempotency_key);


--
-- Name: uk_async_jobs_dedupe_key; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_async_jobs_dedupe_key ON test.async_jobs USING btree (dedupe_key) WHERE (dedupe_key IS NOT NULL);


--
-- Name: uk_claim_preparations_user_idempotency; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_claim_preparations_user_idempotency ON test.claim_preparations USING btree (user_id, idempotency_key) WHERE (idempotency_key IS NOT NULL);


--
-- Name: uk_claim_routes_locale_channel_sort; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_claim_routes_locale_channel_sort ON test.claim_routes USING btree (locale, channel, sort_order);


--
-- Name: uk_documents_request_id; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_documents_request_id ON test.documents USING btree (request_id);


--
-- Name: uk_documents_transfer_receipt_per_transfer; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_documents_transfer_receipt_per_transfer ON test.documents USING btree (transfer_id) WHERE ((document_type)::text = 'TRANSFER_RECEIPT'::text);


--
-- Name: uk_documents_user_type_idempotency; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_documents_user_type_idempotency ON test.documents USING btree (requested_by_user_id, document_type, idempotency_key);


--
-- Name: uk_remittance_recipients_user_wallet; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_remittance_recipients_user_wallet ON test.remittance_recipients USING btree (user_id, lower((wallet_address)::text));


--
-- Name: uk_remittance_transfers_request_id; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_remittance_transfers_request_id ON test.remittance_transfers USING btree (request_id);


--
-- Name: uk_remittance_transfers_safepay_check_id; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_remittance_transfers_safepay_check_id ON test.remittance_transfers USING btree (safepay_check_id);


--
-- Name: uk_remittance_transfers_tx_hash; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_remittance_transfers_tx_hash ON test.remittance_transfers USING btree (lower((tx_hash)::text)) WHERE (tx_hash IS NOT NULL);


--
-- Name: uk_remittance_transfers_user_idempotency; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_remittance_transfers_user_idempotency ON test.remittance_transfers USING btree (user_id, idempotency_key);


--
-- Name: uk_users_email_lower; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_users_email_lower ON test.users USING btree (lower((email)::text));


--
-- Name: uk_vault_accounts_user_token; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_vault_accounts_user_token ON test.vault_accounts USING btree (user_id, token_symbol);


--
-- Name: uk_work_contracts_active_per_workplace; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_work_contracts_active_per_workplace ON test.work_contracts USING btree (workplace_id) WHERE (effective_to IS NULL);


--
-- Name: uk_work_proof_attachments_storage_object_key; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_work_proof_attachments_storage_object_key ON test.work_proof_attachments USING btree (storage_object_key);


--
-- Name: uk_work_proof_records_open_per_user; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_work_proof_records_open_per_user ON test.work_proof_records USING btree (user_id) WHERE ((record_status)::text = 'CHECKED_IN'::text);


--
-- Name: uk_work_proof_records_user_work_date; Type: INDEX; Schema: test; Owner: -
--

CREATE UNIQUE INDEX uk_work_proof_records_user_work_date ON test.work_proof_records USING btree (user_id, work_date);


--
-- Name: advance_requests trg_advance_requests_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_advance_requests_set_updated_at BEFORE UPDATE ON test.advance_requests FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: async_jobs trg_async_jobs_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_async_jobs_set_updated_at BEFORE UPDATE ON test.async_jobs FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: claim_preparations trg_claim_preparations_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_claim_preparations_set_updated_at BEFORE UPDATE ON test.claim_preparations FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: claim_routes trg_claim_routes_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_claim_routes_set_updated_at BEFORE UPDATE ON test.claim_routes FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: documents trg_documents_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_documents_set_updated_at BEFORE UPDATE ON test.documents FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: remittance_recipients trg_remittance_recipients_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_remittance_recipients_set_updated_at BEFORE UPDATE ON test.remittance_recipients FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: remittance_transfers trg_remittance_transfers_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_remittance_transfers_set_updated_at BEFORE UPDATE ON test.remittance_transfers FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: users trg_users_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_users_set_updated_at BEFORE UPDATE ON test.users FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: vault_accounts trg_vault_accounts_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_vault_accounts_set_updated_at BEFORE UPDATE ON test.vault_accounts FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: wage_deposits trg_wage_deposits_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_wage_deposits_set_updated_at BEFORE UPDATE ON test.wage_deposits FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: wage_verifications trg_wage_verifications_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_wage_verifications_set_updated_at BEFORE UPDATE ON test.wage_verifications FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: work_contracts trg_work_contracts_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_work_contracts_set_updated_at BEFORE UPDATE ON test.work_contracts FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: work_proof_modifications trg_work_proof_modifications_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_work_proof_modifications_set_updated_at BEFORE UPDATE ON test.work_proof_modifications FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: work_proof_records trg_work_proof_records_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_work_proof_records_set_updated_at BEFORE UPDATE ON test.work_proof_records FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: workplaces trg_workplaces_set_updated_at; Type: TRIGGER; Schema: test; Owner: -
--

CREATE TRIGGER trg_workplaces_set_updated_at BEFORE UPDATE ON test.workplaces FOR EACH ROW EXECUTE FUNCTION test.set_updated_at();


--
-- Name: work_proofs fk3rodpxe6g6d55ackcg28i4d43; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proofs
    ADD CONSTRAINT fk3rodpxe6g6d55ackcg28i4d43 FOREIGN KEY (contract_id) REFERENCES public.work_contracts(id);


--
-- Name: work_proof_audit_logs fk824q9bqldq51go28gx27eaiie; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proof_audit_logs
    ADD CONSTRAINT fk824q9bqldq51go28gx27eaiie FOREIGN KEY (work_proof_id) REFERENCES public.work_proofs(id);


--
-- Name: advance_payouts fk_advance_payouts_advance_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_payouts
    ADD CONSTRAINT fk_advance_payouts_advance_request FOREIGN KEY (advance_request_id) REFERENCES public.advance_requests(id);


--
-- Name: advance_payouts fk_advance_payouts_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_payouts
    ADD CONSTRAINT fk_advance_payouts_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: recipients fk_recipients_target_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipients
    ADD CONSTRAINT fk_recipients_target_user FOREIGN KEY (target_user_id) REFERENCES public.users(id);


--
-- Name: recipients fk_recipients_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recipients
    ADD CONSTRAINT fk_recipients_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: transfers fk_transfers_recipient; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfers
    ADD CONSTRAINT fk_transfers_recipient FOREIGN KEY (recipient_id) REFERENCES public.recipients(recipient_id);


--
-- Name: transfers fk_transfers_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transfers
    ADD CONSTRAINT fk_transfers_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: user_wallets fk_user_wallets_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_wallets
    ADD CONSTRAINT fk_user_wallets_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: vault_yield_logs fk_vault_yield_logs_position; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_yield_logs
    ADD CONSTRAINT fk_vault_yield_logs_position FOREIGN KEY (position_id) REFERENCES public.vault_positions(id);


--
-- Name: vault_yield_logs fk_vault_yield_logs_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vault_yield_logs
    ADD CONSTRAINT fk_vault_yield_logs_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: document_generation_requests fkaqbo0hf3pywnsrg2fladl7ojf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.document_generation_requests
    ADD CONSTRAINT fkaqbo0hf3pywnsrg2fladl7ojf FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: advance_requests fkb2wp8riqhpu3rhqlp5ewmaylk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_requests
    ADD CONSTRAINT fkb2wp8riqhpu3rhqlp5ewmaylk FOREIGN KEY (workplace_id) REFERENCES public.workplaces(id);


--
-- Name: advance_requests fkdalb49lwnrdbcy52qvkxnkwrc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_requests
    ADD CONSTRAINT fkdalb49lwnrdbcy52qvkxnkwrc FOREIGN KEY (contract_id) REFERENCES public.work_contracts(id);


--
-- Name: claim_preparations fkhpoyyrdlfdtyixkbbsk9lua7p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.claim_preparations
    ADD CONSTRAINT fkhpoyyrdlfdtyixkbbsk9lua7p FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: workplaces fkhumiu2ff1qdapmuetejo5e9fc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workplaces
    ADD CONSTRAINT fkhumiu2ff1qdapmuetejo5e9fc FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: wage_verification_possible_causes fkigpoupf5l1t1uukneb4qtodgy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verification_possible_causes
    ADD CONSTRAINT fkigpoupf5l1t1uukneb4qtodgy FOREIGN KEY (verification_id) REFERENCES public.wage_verifications(id);


--
-- Name: work_proofs fkjggnlxif2tfny4w22l7cociu0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proofs
    ADD CONSTRAINT fkjggnlxif2tfny4w22l7cociu0 FOREIGN KEY (workplace_id) REFERENCES public.workplaces(id);


--
-- Name: correction_decision_audits fkkoyrv1abepkrcwdra32vuul7i; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.correction_decision_audits
    ADD CONSTRAINT fkkoyrv1abepkrcwdra32vuul7i FOREIGN KEY (correction_request_id) REFERENCES public.correction_requests(id);


--
-- Name: advance_requests fkkty5fhc2d4u0y34bq4wjt72i2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.advance_requests
    ADD CONSTRAINT fkkty5fhc2d4u0y34bq4wjt72i2 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: work_proofs fkmseqnn81xfiv7kaijg9lt4tua; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_proofs
    ADD CONSTRAINT fkmseqnn81xfiv7kaijg9lt4tua FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: correction_requests fko0btpinq38ohvrlquvkiw7gsg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.correction_requests
    ADD CONSTRAINT fko0btpinq38ohvrlquvkiw7gsg FOREIGN KEY (work_proof_id) REFERENCES public.work_proofs(id);


--
-- Name: wage_verifications fko36svd7f53kgd19q0tw69kqbw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verifications
    ADD CONSTRAINT fko36svd7f53kgd19q0tw69kqbw FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: wage_verification_record_ids fkpdmbl22gfktluyostbdnfinrw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_verification_record_ids
    ADD CONSTRAINT fkpdmbl22gfktluyostbdnfinrw FOREIGN KEY (verification_id) REFERENCES public.wage_verifications(id);


--
-- Name: work_contracts fkrb33n670qvcxrtc43jf4pqb40; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.work_contracts
    ADD CONSTRAINT fkrb33n670qvcxrtc43jf4pqb40 FOREIGN KEY (workplace_id) REFERENCES public.workplaces(id);


--
-- Name: wage_deposits fkted40fqr2fx1v9et0l83hol9l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wage_deposits
    ADD CONSTRAINT fkted40fqr2fx1v9et0l83hol9l FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: advance_requests advance_requests_contract_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.advance_requests
    ADD CONSTRAINT advance_requests_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES test.work_contracts(id);


--
-- Name: advance_requests advance_requests_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.advance_requests
    ADD CONSTRAINT advance_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: advance_requests advance_requests_workplace_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.advance_requests
    ADD CONSTRAINT advance_requests_workplace_id_fkey FOREIGN KEY (workplace_id) REFERENCES test.workplaces(id);


--
-- Name: async_job_attempts async_job_attempts_job_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.async_job_attempts
    ADD CONSTRAINT async_job_attempts_job_id_fkey FOREIGN KEY (job_id) REFERENCES test.async_jobs(id) ON DELETE CASCADE;


--
-- Name: claim_preparation_checklist_items claim_preparation_checklist_items_preparation_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_checklist_items
    ADD CONSTRAINT claim_preparation_checklist_items_preparation_id_fkey FOREIGN KEY (preparation_id) REFERENCES test.claim_preparations(id) ON DELETE CASCADE;


--
-- Name: claim_preparation_document_links claim_preparation_document_links_document_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_document_links
    ADD CONSTRAINT claim_preparation_document_links_document_id_fkey FOREIGN KEY (document_id) REFERENCES test.documents(id);


--
-- Name: claim_preparation_document_links claim_preparation_document_links_preparation_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_document_links
    ADD CONSTRAINT claim_preparation_document_links_preparation_id_fkey FOREIGN KEY (preparation_id) REFERENCES test.claim_preparations(id) ON DELETE CASCADE;


--
-- Name: claim_preparation_route_snapshots claim_preparation_route_snapshots_preparation_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparation_route_snapshots
    ADD CONSTRAINT claim_preparation_route_snapshots_preparation_id_fkey FOREIGN KEY (preparation_id) REFERENCES test.claim_preparations(id) ON DELETE CASCADE;


--
-- Name: claim_preparations claim_preparations_claim_kit_document_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparations
    ADD CONSTRAINT claim_preparations_claim_kit_document_id_fkey FOREIGN KEY (claim_kit_document_id) REFERENCES test.documents(id);


--
-- Name: claim_preparations claim_preparations_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparations
    ADD CONSTRAINT claim_preparations_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: claim_preparations claim_preparations_wage_verification_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.claim_preparations
    ADD CONSTRAINT claim_preparations_wage_verification_id_fkey FOREIGN KEY (wage_verification_id) REFERENCES test.wage_verifications(id);


--
-- Name: documents documents_requested_by_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.documents
    ADD CONSTRAINT documents_requested_by_user_id_fkey FOREIGN KEY (requested_by_user_id) REFERENCES test.users(id);


--
-- Name: documents documents_transfer_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.documents
    ADD CONSTRAINT documents_transfer_id_fkey FOREIGN KEY (transfer_id) REFERENCES test.remittance_transfers(id);


--
-- Name: documents documents_wage_verification_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.documents
    ADD CONSTRAINT documents_wage_verification_id_fkey FOREIGN KEY (wage_verification_id) REFERENCES test.wage_verifications(id);


--
-- Name: documents documents_workplace_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.documents
    ADD CONSTRAINT documents_workplace_id_fkey FOREIGN KEY (workplace_id) REFERENCES test.workplaces(id);


--
-- Name: remittance_recipients remittance_recipients_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_recipients
    ADD CONSTRAINT remittance_recipients_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: remittance_transfer_events remittance_transfer_events_transfer_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_transfer_events
    ADD CONSTRAINT remittance_transfer_events_transfer_id_fkey FOREIGN KEY (transfer_id) REFERENCES test.remittance_transfers(id) ON DELETE CASCADE;


--
-- Name: remittance_transfers remittance_transfers_recipient_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_transfers
    ADD CONSTRAINT remittance_transfers_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES test.remittance_recipients(id);


--
-- Name: remittance_transfers remittance_transfers_safepay_check_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_transfers
    ADD CONSTRAINT remittance_transfers_safepay_check_id_fkey FOREIGN KEY (safepay_check_id) REFERENCES test.safepay_transfer_checks(id);


--
-- Name: remittance_transfers remittance_transfers_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.remittance_transfers
    ADD CONSTRAINT remittance_transfers_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: safepay_transfer_check_reasons safepay_transfer_check_reasons_safepay_check_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.safepay_transfer_check_reasons
    ADD CONSTRAINT safepay_transfer_check_reasons_safepay_check_id_fkey FOREIGN KEY (safepay_check_id) REFERENCES test.safepay_transfer_checks(id) ON DELETE CASCADE;


--
-- Name: safepay_transfer_checks safepay_transfer_checks_recipient_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.safepay_transfer_checks
    ADD CONSTRAINT safepay_transfer_checks_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES test.remittance_recipients(id);


--
-- Name: safepay_transfer_checks safepay_transfer_checks_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.safepay_transfer_checks
    ADD CONSTRAINT safepay_transfer_checks_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: vault_accounts vault_accounts_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.vault_accounts
    ADD CONSTRAINT vault_accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: vault_ledger_entries vault_ledger_entries_account_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.vault_ledger_entries
    ADD CONSTRAINT vault_ledger_entries_account_id_fkey FOREIGN KEY (account_id) REFERENCES test.vault_accounts(id);


--
-- Name: wage_deposits wage_deposits_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_deposits
    ADD CONSTRAINT wage_deposits_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: wage_verification_cause_snapshots wage_verification_cause_snapshots_verification_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_cause_snapshots
    ADD CONSTRAINT wage_verification_cause_snapshots_verification_id_fkey FOREIGN KEY (verification_id) REFERENCES test.wage_verifications(id) ON DELETE CASCADE;


--
-- Name: wage_verification_record_links wage_verification_record_links_verification_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_record_links
    ADD CONSTRAINT wage_verification_record_links_verification_id_fkey FOREIGN KEY (verification_id) REFERENCES test.wage_verifications(id) ON DELETE CASCADE;


--
-- Name: wage_verification_record_links wage_verification_record_links_work_proof_record_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verification_record_links
    ADD CONSTRAINT wage_verification_record_links_work_proof_record_id_fkey FOREIGN KEY (work_proof_record_id) REFERENCES test.work_proof_records(id);


--
-- Name: wage_verifications wage_verifications_contract_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verifications
    ADD CONSTRAINT wage_verifications_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES test.work_contracts(id);


--
-- Name: wage_verifications wage_verifications_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verifications
    ADD CONSTRAINT wage_verifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: wage_verifications wage_verifications_workplace_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.wage_verifications
    ADD CONSTRAINT wage_verifications_workplace_id_fkey FOREIGN KEY (workplace_id) REFERENCES test.workplaces(id);


--
-- Name: work_contracts work_contracts_workplace_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_contracts
    ADD CONSTRAINT work_contracts_workplace_id_fkey FOREIGN KEY (workplace_id) REFERENCES test.workplaces(id);


--
-- Name: work_proof_attachments work_proof_attachments_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_attachments
    ADD CONSTRAINT work_proof_attachments_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: work_proof_modification_attachments work_proof_modification_attachments_attachment_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modification_attachments
    ADD CONSTRAINT work_proof_modification_attachments_attachment_id_fkey FOREIGN KEY (attachment_id) REFERENCES test.work_proof_attachments(id);


--
-- Name: work_proof_modification_attachments work_proof_modification_attachments_modification_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modification_attachments
    ADD CONSTRAINT work_proof_modification_attachments_modification_id_fkey FOREIGN KEY (modification_id) REFERENCES test.work_proof_modifications(id) ON DELETE CASCADE;


--
-- Name: work_proof_modifications work_proof_modifications_modified_by_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modifications
    ADD CONSTRAINT work_proof_modifications_modified_by_user_id_fkey FOREIGN KEY (modified_by_user_id) REFERENCES test.users(id);


--
-- Name: work_proof_modifications work_proof_modifications_record_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modifications
    ADD CONSTRAINT work_proof_modifications_record_id_fkey FOREIGN KEY (record_id) REFERENCES test.work_proof_records(id);


--
-- Name: work_proof_modifications work_proof_modifications_reviewed_by_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_modifications
    ADD CONSTRAINT work_proof_modifications_reviewed_by_user_id_fkey FOREIGN KEY (reviewed_by_user_id) REFERENCES test.users(id);


--
-- Name: work_proof_records work_proof_records_contract_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_records
    ADD CONSTRAINT work_proof_records_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES test.work_contracts(id);


--
-- Name: work_proof_records work_proof_records_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_records
    ADD CONSTRAINT work_proof_records_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- Name: work_proof_records work_proof_records_workplace_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.work_proof_records
    ADD CONSTRAINT work_proof_records_workplace_id_fkey FOREIGN KEY (workplace_id) REFERENCES test.workplaces(id);


--
-- Name: workplaces workplaces_user_id_fkey; Type: FK CONSTRAINT; Schema: test; Owner: -
--

ALTER TABLE ONLY test.workplaces
    ADD CONSTRAINT workplaces_user_id_fkey FOREIGN KEY (user_id) REFERENCES test.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict GUR3T3FyCgweW4LLjM30ebEAyleoYPImQWn6gff7uNeXjjgy7z5OCHsU1n5YMH4

