USE [Vhotel_HN_test];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF COL_LENGTH('dbo.cash_transactions', 'document_code') IS NULL
    ALTER TABLE dbo.cash_transactions ADD document_code VARCHAR(30) NULL;
IF COL_LENGTH('dbo.cash_transactions', 'fund_method') IS NULL
    ALTER TABLE dbo.cash_transactions ADD fund_method VARCHAR(10) NULL;
GO

UPDATE dbo.cash_transactions
SET document_code = CASE
        WHEN transaction_type = 'INCOME' THEN CONCAT('PT-MIG-', cash_transaction_id)
        ELSE CONCAT('PC-MIG-', cash_transaction_id)
    END
WHERE document_code IS NULL;

UPDATE dbo.cash_transactions
SET fund_method = 'CASH'
WHERE fund_method IS NULL;
GO

ALTER TABLE dbo.cash_transactions ALTER COLUMN document_code VARCHAR(30) NOT NULL;
ALTER TABLE dbo.cash_transactions ALTER COLUMN fund_method VARCHAR(10) NOT NULL;
ALTER TABLE dbo.cash_transactions ALTER COLUMN amount NUMERIC(15,0) NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_cash_transactions_document_code'
      AND object_id = OBJECT_ID('dbo.cash_transactions')
)
    CREATE UNIQUE INDEX UX_cash_transactions_document_code ON dbo.cash_transactions(document_code);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_cash_transactions_source'
      AND object_id = OBJECT_ID('dbo.cash_transactions')
)
    CREATE UNIQUE INDEX UX_cash_transactions_source
    ON dbo.cash_transactions(source_type, source_id)
    WHERE source_id IS NOT NULL;
GO

IF OBJECT_ID(N'dbo.payments', N'U') IS NOT NULL
BEGIN
    ALTER TABLE dbo.payments ALTER COLUMN amount NUMERIC(15,0) NOT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'UX_payments_transaction_ref'
          AND object_id = OBJECT_ID('dbo.payments')
    )
        CREATE UNIQUE INDEX UX_payments_transaction_ref
        ON dbo.payments(transaction_ref)
        WHERE transaction_ref IS NOT NULL;
END;
GO

IF COL_LENGTH('dbo.hotel_fund_settings', 'opening_cash_balance') IS NULL
    ALTER TABLE dbo.hotel_fund_settings ADD opening_cash_balance NUMERIC(15,0) NULL;
IF COL_LENGTH('dbo.hotel_fund_settings', 'opening_transfer_balance') IS NULL
    ALTER TABLE dbo.hotel_fund_settings ADD opening_transfer_balance NUMERIC(15,0) NULL;
IF COL_LENGTH('dbo.hotel_fund_settings', 'opening_card_balance') IS NULL
    ALTER TABLE dbo.hotel_fund_settings ADD opening_card_balance NUMERIC(15,0) NULL;
GO

UPDATE dbo.hotel_fund_settings
SET opening_cash_balance = ISNULL(opening_cash_balance, opening_balance),
    opening_transfer_balance = ISNULL(opening_transfer_balance, 0),
    opening_card_balance = ISNULL(opening_card_balance, 0);
GO

ALTER TABLE dbo.hotel_fund_settings ALTER COLUMN opening_balance NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.hotel_fund_settings ALTER COLUMN opening_cash_balance NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.hotel_fund_settings ALTER COLUMN opening_transfer_balance NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.hotel_fund_settings ALTER COLUMN opening_card_balance NUMERIC(15,0) NOT NULL;
GO

ALTER TABLE dbo.services ALTER COLUMN price NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.room_types ALTER COLUMN base_price NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.room_types', 'extra_bed_price') IS NOT NULL
    ALTER TABLE dbo.room_types ALTER COLUMN extra_bed_price NUMERIC(15,0) NULL;
GO

ALTER TABLE dbo.inventory_items ALTER COLUMN unit_cost NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.inventory_receipts ALTER COLUMN unit_cost NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.inventory_receipts ALTER COLUMN subtotal NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.inventory_receipts ALTER COLUMN vat_amount NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.inventory_receipts ALTER COLUMN total_cost NUMERIC(15,0) NOT NULL;
GO

ALTER TABLE dbo.bookings ALTER COLUMN discount_amount NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.bookings ALTER COLUMN total_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.bookings', 'room_subtotal') IS NOT NULL
    ALTER TABLE dbo.bookings ALTER COLUMN room_subtotal NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.bookings', 'service_subtotal') IS NOT NULL
    ALTER TABLE dbo.bookings ALTER COLUMN service_subtotal NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.bookings', 'service_charge_total') IS NOT NULL
    ALTER TABLE dbo.bookings ALTER COLUMN service_charge_total NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.bookings', 'vat_total') IS NOT NULL
    ALTER TABLE dbo.bookings ALTER COLUMN vat_total NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.bookings', 'grand_total') IS NOT NULL
    ALTER TABLE dbo.bookings ALTER COLUMN grand_total NUMERIC(15,0) NOT NULL;
GO

ALTER TABLE dbo.booking_details ALTER COLUMN price_per_night NUMERIC(15,0) NOT NULL;
ALTER TABLE dbo.booking_details ALTER COLUMN subtotal NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.booking_details', 'service_charge_amount') IS NOT NULL
    ALTER TABLE dbo.booking_details ALTER COLUMN service_charge_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.booking_details', 'vat_amount') IS NOT NULL
    ALTER TABLE dbo.booking_details ALTER COLUMN vat_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.booking_details', 'total_amount') IS NOT NULL
    ALTER TABLE dbo.booking_details ALTER COLUMN total_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.booking_details', 'extra_bed_price') IS NOT NULL
    ALTER TABLE dbo.booking_details ALTER COLUMN extra_bed_price NUMERIC(15,0) NULL;
IF COL_LENGTH('dbo.booking_details', 'extra_bed_total') IS NOT NULL
    ALTER TABLE dbo.booking_details ALTER COLUMN extra_bed_total NUMERIC(15,0) NULL;
GO

ALTER TABLE dbo.folio_items ALTER COLUMN amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.folio_items', 'base_amount') IS NOT NULL
    ALTER TABLE dbo.folio_items ALTER COLUMN base_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.folio_items', 'service_charge_amount') IS NOT NULL
    ALTER TABLE dbo.folio_items ALTER COLUMN service_charge_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.folio_items', 'vat_amount') IS NOT NULL
    ALTER TABLE dbo.folio_items ALTER COLUMN vat_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.folio_items', 'total_amount') IS NOT NULL
    ALTER TABLE dbo.folio_items ALTER COLUMN total_amount NUMERIC(15,0) NOT NULL;
IF COL_LENGTH('dbo.folio_items', 'unit_price') IS NOT NULL
    ALTER TABLE dbo.folio_items ALTER COLUMN unit_price NUMERIC(15,0) NOT NULL;
GO

SELECT 'Fund/payment schema migrated' AS message;
GO
