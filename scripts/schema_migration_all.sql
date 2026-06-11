USE [Vhotel_HN];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

/* =========================================================
   1. Promotions schema
   ========================================================= */

IF COL_LENGTH('dbo.promotions', 'show_on_homepage') IS NULL
BEGIN
    ALTER TABLE dbo.promotions
    ADD show_on_homepage BIT NOT NULL CONSTRAINT DF_promotions_show_on_homepage DEFAULT 0;
END;
GO

IF COL_LENGTH('dbo.promotions', 'featured') IS NULL
BEGIN
    ALTER TABLE dbo.promotions
    ADD featured BIT NOT NULL CONSTRAINT DF_promotions_featured DEFAULT 0;
END;
GO

IF COL_LENGTH('dbo.promotions', 'discount_type') IS NOT NULL
BEGIN
    ALTER TABLE dbo.promotions DROP COLUMN discount_type;
END;
GO

IF COL_LENGTH('dbo.promotions', 'max_discount') IS NOT NULL
BEGIN
    ALTER TABLE dbo.promotions DROP COLUMN max_discount;
END;
GO

IF COL_LENGTH('dbo.promotions', 'discount_value') IS NOT NULL
   AND COL_LENGTH('dbo.promotions', 'discount_amount') IS NULL
BEGIN
    EXEC sp_rename 'dbo.promotions.discount_value', 'discount_amount', 'COLUMN';
END;
GO

ALTER TABLE dbo.promotions
ALTER COLUMN name NVARCHAR(200) NOT NULL;
GO

ALTER TABLE dbo.promotions
ALTER COLUMN description NVARCHAR(300) NULL;
GO

/* =========================================================
   2. Tax, service charge, and VND money columns
   ========================================================= */

IF OBJECT_ID(N'dbo.financial_charge_settings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.financial_charge_settings (
        setting_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        service_charge_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_financial_charge_settings_service_charge_rate DEFAULT 5,
        vat_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_financial_charge_settings_vat_rate DEFAULT 8,
        inventory_vat_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_financial_charge_settings_inventory_vat_rate DEFAULT 8,
        tax_on_service_charge BIT NOT NULL CONSTRAINT DF_financial_charge_settings_tax_on_service_charge DEFAULT 1,
        price_display_mode VARCHAR(15) NOT NULL CONSTRAINT DF_financial_charge_settings_price_display_mode DEFAULT 'PLUS_PLUS',
        effective_from DATE NOT NULL,
        effective_to DATE NULL,
        is_active BIT NOT NULL CONSTRAINT DF_financial_charge_settings_is_active DEFAULT 1,
        created_by BIGINT NULL,
        created_at DATETIMEOFFSET(7) NOT NULL
    );
END;
GO

IF COL_LENGTH('dbo.booking_details', 'service_charge_rate') IS NULL
    ALTER TABLE dbo.booking_details ADD service_charge_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_booking_details_service_charge_rate DEFAULT 0;
IF COL_LENGTH('dbo.booking_details', 'service_charge_amount') IS NULL
    ALTER TABLE dbo.booking_details ADD service_charge_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_booking_details_service_charge_amount DEFAULT 0;
IF COL_LENGTH('dbo.booking_details', 'vat_rate') IS NULL
    ALTER TABLE dbo.booking_details ADD vat_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_booking_details_vat_rate DEFAULT 0;
IF COL_LENGTH('dbo.booking_details', 'vat_amount') IS NULL
    ALTER TABLE dbo.booking_details ADD vat_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_booking_details_vat_amount DEFAULT 0;
IF COL_LENGTH('dbo.booking_details', 'total_amount') IS NULL
    ALTER TABLE dbo.booking_details ADD total_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_booking_details_total_amount DEFAULT 0;
GO

IF COL_LENGTH('dbo.bookings', 'room_subtotal') IS NULL
    ALTER TABLE dbo.bookings ADD room_subtotal NUMERIC(15,0) NOT NULL CONSTRAINT DF_bookings_room_subtotal DEFAULT 0;
IF COL_LENGTH('dbo.bookings', 'service_subtotal') IS NULL
    ALTER TABLE dbo.bookings ADD service_subtotal NUMERIC(15,0) NOT NULL CONSTRAINT DF_bookings_service_subtotal DEFAULT 0;
IF COL_LENGTH('dbo.bookings', 'service_charge_total') IS NULL
    ALTER TABLE dbo.bookings ADD service_charge_total NUMERIC(15,0) NOT NULL CONSTRAINT DF_bookings_service_charge_total DEFAULT 0;
IF COL_LENGTH('dbo.bookings', 'vat_total') IS NULL
    ALTER TABLE dbo.bookings ADD vat_total NUMERIC(15,0) NOT NULL CONSTRAINT DF_bookings_vat_total DEFAULT 0;
IF COL_LENGTH('dbo.bookings', 'grand_total') IS NULL
    ALTER TABLE dbo.bookings ADD grand_total NUMERIC(15,0) NOT NULL CONSTRAINT DF_bookings_grand_total DEFAULT 0;
GO

IF COL_LENGTH('dbo.folio_items', 'base_amount') IS NULL
    ALTER TABLE dbo.folio_items ADD base_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_folio_items_base_amount DEFAULT 0;
IF COL_LENGTH('dbo.folio_items', 'service_charge_rate') IS NULL
    ALTER TABLE dbo.folio_items ADD service_charge_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_folio_items_service_charge_rate DEFAULT 0;
IF COL_LENGTH('dbo.folio_items', 'service_charge_amount') IS NULL
    ALTER TABLE dbo.folio_items ADD service_charge_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_folio_items_service_charge_amount DEFAULT 0;
IF COL_LENGTH('dbo.folio_items', 'vat_rate') IS NULL
    ALTER TABLE dbo.folio_items ADD vat_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_folio_items_vat_rate DEFAULT 0;
IF COL_LENGTH('dbo.folio_items', 'vat_amount') IS NULL
    ALTER TABLE dbo.folio_items ADD vat_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_folio_items_vat_amount DEFAULT 0;
IF COL_LENGTH('dbo.folio_items', 'total_amount') IS NULL
    ALTER TABLE dbo.folio_items ADD total_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_folio_items_total_amount DEFAULT 0;
IF COL_LENGTH('dbo.folio_items', 'price_display_mode') IS NULL
    ALTER TABLE dbo.folio_items ADD price_display_mode VARCHAR(15) NOT NULL CONSTRAINT DF_folio_items_price_display_mode DEFAULT 'PLUS_PLUS';
GO

IF COL_LENGTH('dbo.inventory_receipts', 'subtotal') IS NULL
    ALTER TABLE dbo.inventory_receipts ADD subtotal NUMERIC(15,0) NOT NULL CONSTRAINT DF_inventory_receipts_subtotal DEFAULT 0;
IF COL_LENGTH('dbo.inventory_receipts', 'vat_rate') IS NULL
    ALTER TABLE dbo.inventory_receipts ADD vat_rate NUMERIC(5,2) NOT NULL CONSTRAINT DF_inventory_receipts_vat_rate DEFAULT 0;
IF COL_LENGTH('dbo.inventory_receipts', 'vat_amount') IS NULL
    ALTER TABLE dbo.inventory_receipts ADD vat_amount NUMERIC(15,0) NOT NULL CONSTRAINT DF_inventory_receipts_vat_amount DEFAULT 0;
GO

UPDATE dbo.booking_details
SET total_amount = CASE WHEN total_amount = 0 THEN subtotal ELSE total_amount END;

UPDATE dbo.bookings
SET grand_total = CASE WHEN grand_total = 0 THEN total_amount ELSE grand_total END;

UPDATE dbo.folio_items
SET base_amount = CASE WHEN base_amount = 0 THEN amount ELSE base_amount END,
    total_amount = CASE WHEN total_amount = 0 THEN amount ELSE total_amount END;

UPDATE dbo.inventory_receipts
SET subtotal = CASE WHEN subtotal = 0 THEN quantity * unit_cost ELSE subtotal END,
    vat_rate = CASE WHEN vat_rate = 0 THEN 8 ELSE vat_rate END,
    vat_amount = CASE WHEN vat_amount = 0 THEN ROUND(quantity * unit_cost * 0.08, 0) ELSE vat_amount END,
    total_cost = CASE WHEN total_cost = quantity * unit_cost THEN ROUND(quantity * unit_cost * 1.08, 0) ELSE total_cost END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.financial_charge_settings WHERE is_active = 1)
BEGIN
    DECLARE @managerId BIGINT = (SELECT TOP 1 u.user_id
                                 FROM dbo.users u
                                 JOIN dbo.user_roles ur ON ur.user_id = u.user_id
                                 JOIN dbo.roles r ON r.role_id = ur.role_id
                                 WHERE r.role_name = 'MANAGER'
                                 ORDER BY u.user_id);

    INSERT INTO dbo.financial_charge_settings (
        service_charge_rate, vat_rate, inventory_vat_rate, tax_on_service_charge,
        price_display_mode, effective_from, effective_to, is_active, created_by, created_at
    )
    VALUES (5.00, 8.00, 8.00, 1, 'PLUS_PLUS', CAST(GETDATE() AS date), NULL, 1, @managerId, SYSDATETIMEOFFSET());
END;
GO

/* =========================================================
   3. Fund/payment audit, duplicate guards, and fund methods
   ========================================================= */

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

SELECT 'Schema migration completed' AS message;
GO
