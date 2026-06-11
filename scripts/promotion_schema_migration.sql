USE [Vhotel_HN_test];
GO

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
