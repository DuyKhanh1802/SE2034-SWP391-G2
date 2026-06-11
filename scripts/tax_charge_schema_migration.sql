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
