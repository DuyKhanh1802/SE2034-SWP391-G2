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

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @roleConstraintName sysname;
DECLARE @sql nvarchar(max);

SELECT @roleConstraintName = name
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.roles')
  AND definition LIKE N'%[role_name]%';

IF @roleConstraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE dbo.roles DROP CONSTRAINT [' + @roleConstraintName + N']';
    EXEC sp_executesql @sql;
END;

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_name = 'SYSTEM_ADMIN')
    INSERT INTO dbo.roles (role_name) VALUES ('SYSTEM_ADMIN');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_name = 'HOTEL_ADMIN')
    INSERT INTO dbo.roles (role_name) VALUES ('HOTEL_ADMIN');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_name = 'MANAGER')
    INSERT INTO dbo.roles (role_name) VALUES ('MANAGER');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_name = 'RECEPTIONIST')
    INSERT INTO dbo.roles (role_name) VALUES ('RECEPTIONIST');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_name = 'GUEST')
    INSERT INTO dbo.roles (role_name) VALUES ('GUEST');

IF @roleConstraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE dbo.roles ADD CONSTRAINT [' + @roleConstraintName + N'] CHECK ([role_name] IN (''SYSTEM_ADMIN'', ''HOTEL_ADMIN'', ''MANAGER'', ''RECEPTIONIST'', ''GUEST''))';
    EXEC sp_executesql @sql;
END;

DECLARE @passwordHash nvarchar(255) = N'$2a$10$U30Tua8MbwDGz48W50NJrOPGoCNFGqHfw1mz5F/oKD/WHqU0OxR7u'; -- 123456
DECLARE @now datetime2 = SYSDATETIME();

IF NOT EXISTS (SELECT 1 FROM dbo.countries WHERE country_code = 'VN')
BEGIN
    INSERT INTO dbo.countries (country_name, country_code, phone_code, is_active, created_at)
    VALUES (N'Vietnam', 'VN', '+84', 1, @now);
END;

DECLARE @countryId bigint = (SELECT TOP 1 country_id FROM dbo.countries WHERE country_code = 'VN');

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'manager@test.com')
BEGIN
    INSERT INTO dbo.users (
        user_type, approval_status, first_name, last_name, email, phone, password_hash,
        country_id, total_stays, total_spent, is_active, is_deleted, created_at, updated_at
    )
    VALUES (
        'STAFF', 'APPROVED', N'Manager', N'Test', N'manager@test.com', '0900000001', @passwordHash,
        @countryId, 0, 0, 1, 0, @now, @now
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'hoteladmin@test.com')
BEGIN
    INSERT INTO dbo.users (
        user_type, approval_status, first_name, last_name, email, phone, password_hash,
        country_id, total_stays, total_spent, is_active, is_deleted, created_at, updated_at
    )
    VALUES (
        'STAFF', 'APPROVED', N'Hotel Admin', N'Test', N'hoteladmin@test.com', '0900000002', @passwordHash,
        @countryId, 0, 0, 1, 0, @now, @now
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'receptionist@test.com')
BEGIN
    INSERT INTO dbo.users (
        user_type, approval_status, first_name, last_name, email, phone, password_hash,
        country_id, total_stays, total_spent, is_active, is_deleted, created_at, updated_at
    )
    VALUES (
        'STAFF', 'APPROVED', N'Receptionist', N'Test', N'receptionist@test.com', '0900000003', @passwordHash,
        @countryId, 0, 0, 1, 0, @now, @now
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'guest@test.com')
BEGIN
    INSERT INTO dbo.users (
        user_type, approval_status, first_name, last_name, email, phone, password_hash,
        country_id, total_stays, total_spent, is_active, is_deleted, created_at, updated_at
    )
    VALUES (
        'GUEST', 'APPROVED', N'Guest', N'Test', N'guest@test.com', '0900000004', @passwordHash,
        @countryId, 0, 0, 1, 0, @now, @now
    );
END;

DECLARE @managerId bigint = (SELECT user_id FROM dbo.users WHERE email = N'manager@test.com');
DECLARE @hotelAdminId bigint = (SELECT user_id FROM dbo.users WHERE email = N'hoteladmin@test.com');
DECLARE @receptionistId bigint = (SELECT user_id FROM dbo.users WHERE email = N'receptionist@test.com');
DECLARE @guestId bigint = (SELECT user_id FROM dbo.users WHERE email = N'guest@test.com');

IF OBJECT_ID(N'dbo.financial_charge_settings', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.financial_charge_settings WHERE is_active = 1)
BEGIN
    INSERT INTO dbo.financial_charge_settings (
        service_charge_rate, vat_rate, inventory_vat_rate, tax_on_service_charge,
        price_display_mode, effective_from, effective_to, is_active, created_by, created_at
    )
    VALUES (5.00, 8.00, 8.00, 1, 'PLUS_PLUS', CAST(GETDATE() AS date), NULL, 1, @managerId, @now);
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.user_roles ur
    JOIN dbo.roles r ON r.role_id = ur.role_id
    WHERE ur.user_id = @managerId AND r.role_name = 'MANAGER'
)
    INSERT INTO dbo.user_roles (user_id, role_id, assigned_at, assigned_by)
    SELECT @managerId, role_id, @now, @managerId FROM dbo.roles WHERE role_name = 'MANAGER';

IF NOT EXISTS (
    SELECT 1 FROM dbo.user_roles ur
    JOIN dbo.roles r ON r.role_id = ur.role_id
    WHERE ur.user_id = @hotelAdminId AND r.role_name = 'HOTEL_ADMIN'
)
    INSERT INTO dbo.user_roles (user_id, role_id, assigned_at, assigned_by)
    SELECT @hotelAdminId, role_id, @now, @hotelAdminId FROM dbo.roles WHERE role_name = 'HOTEL_ADMIN';

IF NOT EXISTS (
    SELECT 1 FROM dbo.user_roles ur
    JOIN dbo.roles r ON r.role_id = ur.role_id
    WHERE ur.user_id = @receptionistId AND r.role_name = 'RECEPTIONIST'
)
    INSERT INTO dbo.user_roles (user_id, role_id, assigned_at, assigned_by)
    SELECT @receptionistId, role_id, @now, @managerId FROM dbo.roles WHERE role_name = 'RECEPTIONIST';

IF NOT EXISTS (
    SELECT 1 FROM dbo.user_roles ur
    JOIN dbo.roles r ON r.role_id = ur.role_id
    WHERE ur.user_id = @guestId AND r.role_name = 'GUEST'
)
    INSERT INTO dbo.user_roles (user_id, role_id, assigned_at, assigned_by)
    SELECT @guestId, role_id, @now, @managerId FROM dbo.roles WHERE role_name = 'GUEST';

IF NOT EXISTS (SELECT 1 FROM dbo.room_types WHERE name = 'STANDARD')
    INSERT INTO dbo.room_types (
        name, base_price, max_adults, max_children, room_size, description,
        allow_extra_bed, max_extra_beds, extra_bed_price, extra_bed_note,
        is_deleted, created_at, updated_at
    )
    VALUES ('STANDARD', 800000, 2, 1, 28, N'Phòng tiêu chuẩn', 0, 0, 0, NULL, 0, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.room_types WHERE name = 'DELUXE')
    INSERT INTO dbo.room_types (
        name, base_price, max_adults, max_children, room_size, description,
        allow_extra_bed, max_extra_beds, extra_bed_price, extra_bed_note,
        is_deleted, created_at, updated_at
    )
    VALUES ('DELUXE', 1200000, 2, 2, 36, N'Phòng deluxe', 1, 1, 300000, N'Có thể kê thêm 1 giường phụ', 0, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.room_types WHERE name = 'SUITE')
    INSERT INTO dbo.room_types (
        name, base_price, max_adults, max_children, room_size, description,
        allow_extra_bed, max_extra_beds, extra_bed_price, extra_bed_note,
        is_deleted, created_at, updated_at
    )
    VALUES ('SUITE', 2500000, 4, 2, 58, N'Phòng suite', 1, 2, 350000, N'Có thể kê thêm tối đa 2 giường phụ', 0, @now, @now);

DECLARE @standardRoomTypeId bigint = (SELECT room_type_id FROM dbo.room_types WHERE name = 'STANDARD');
DECLARE @deluxeRoomTypeId bigint = (SELECT room_type_id FROM dbo.room_types WHERE name = 'DELUXE');

IF NOT EXISTS (SELECT 1 FROM dbo.rooms WHERE room_number = '101')
    INSERT INTO dbo.rooms (room_type_id, room_number, floor, view_type, status, note, is_deleted, created_at, updated_at)
    VALUES (@standardRoomTypeId, '101', 1, 'CITY', 'AVAILABLE', NULL, 0, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.rooms WHERE room_number = '102')
    INSERT INTO dbo.rooms (room_type_id, room_number, floor, view_type, status, note, is_deleted, created_at, updated_at)
    VALUES (@standardRoomTypeId, '102', 1, 'CITY', 'OCCUPIED', NULL, 0, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.rooms WHERE room_number = '201')
    INSERT INTO dbo.rooms (room_type_id, room_number, floor, view_type, status, note, is_deleted, created_at, updated_at)
    VALUES (@deluxeRoomTypeId, '201', 2, 'GARDEN', 'AVAILABLE', NULL, 0, @now, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.service_categories WHERE type = 'FOOD' AND name = N'Ẩm thực')
    INSERT INTO dbo.service_categories (name, type, description, is_deleted)
    VALUES (N'Ẩm thực', 'FOOD', N'Dịch vụ đồ ăn và đồ uống', 0);
IF NOT EXISTS (SELECT 1 FROM dbo.service_categories WHERE type = 'SPA' AND name = N'Spa')
    INSERT INTO dbo.service_categories (name, type, description, is_deleted)
    VALUES (N'Spa', 'SPA', N'Dịch vụ chăm sóc sức khỏe', 0);

DECLARE @foodCategoryId bigint = (SELECT TOP 1 service_category_id FROM dbo.service_categories WHERE type = 'FOOD' ORDER BY service_category_id);
DECLARE @spaCategoryId bigint = (SELECT TOP 1 service_category_id FROM dbo.service_categories WHERE type = 'SPA' ORDER BY service_category_id);

IF NOT EXISTS (SELECT 1 FROM dbo.services WHERE name = N'Nước suối minibar')
    INSERT INTO dbo.services (category_id, name, description, price, is_available, is_deleted, created_at, updated_at)
    VALUES (@foodCategoryId, N'Nước suối minibar', N'Nước suối phục vụ minibar', 25000, 1, 0, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.services WHERE name = N'Massage thư giãn')
    INSERT INTO dbo.services (category_id, name, description, price, is_available, is_deleted, created_at, updated_at)
    VALUES (@spaCategoryId, N'Massage thư giãn', N'Dịch vụ massage 60 phút', 450000, 1, 0, @now, @now);

DECLARE @waterServiceId bigint = (SELECT service_id FROM dbo.services WHERE name = N'Nước suối minibar');
DECLARE @spaServiceId bigint = (SELECT service_id FROM dbo.services WHERE name = N'Massage thư giãn');

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE item_code = 'IT-SEED-WATER')
    INSERT INTO dbo.inventory_items (
        item_code, name, category, unit, opening_quantity, current_quantity, minimum_quantity, unit_cost, is_deleted, created_at, updated_at
    )
    VALUES ('IT-SEED-WATER', N'Nước suối 500ml', N'Minibar', N'chai', 100, 92, 20, 8000, 0, @now, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_items WHERE item_code = 'IT-SEED-TOWEL')
    INSERT INTO dbo.inventory_items (
        item_code, name, category, unit, opening_quantity, current_quantity, minimum_quantity, unit_cost, is_deleted, created_at, updated_at
    )
    VALUES ('IT-SEED-TOWEL', N'Khăn spa', N'Spa', N'cái', 30, 12, 15, 35000, 0, @now, @now);

DECLARE @waterItemId bigint = (SELECT inventory_item_id FROM dbo.inventory_items WHERE item_code = 'IT-SEED-WATER');
DECLARE @towelItemId bigint = (SELECT inventory_item_id FROM dbo.inventory_items WHERE item_code = 'IT-SEED-TOWEL');

IF NOT EXISTS (SELECT 1 FROM dbo.service_inventory_mappings WHERE service_id = @waterServiceId AND inventory_item_id = @waterItemId)
    INSERT INTO dbo.service_inventory_mappings (service_id, inventory_item_id, quantity_per_use)
    VALUES (@waterServiceId, @waterItemId, 1);
IF NOT EXISTS (SELECT 1 FROM dbo.service_inventory_mappings WHERE service_id = @spaServiceId AND inventory_item_id = @towelItemId)
    INSERT INTO dbo.service_inventory_mappings (service_id, inventory_item_id, quantity_per_use)
    VALUES (@spaServiceId, @towelItemId, 1);

IF OBJECT_ID(N'dbo.room_refresh_inventory_mappings', N'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.room_refresh_inventory_mappings WHERE room_type_id = @standardRoomTypeId AND inventory_item_id = @waterItemId)
        INSERT INTO dbo.room_refresh_inventory_mappings (room_type_id, inventory_item_id, quantity_per_refresh)
        VALUES (@standardRoomTypeId, @waterItemId, 2);
    IF NOT EXISTS (SELECT 1 FROM dbo.room_refresh_inventory_mappings WHERE room_type_id = @deluxeRoomTypeId AND inventory_item_id = @waterItemId)
        INSERT INTO dbo.room_refresh_inventory_mappings (room_type_id, inventory_item_id, quantity_per_refresh)
        VALUES (@deluxeRoomTypeId, @waterItemId, 3);
    IF NOT EXISTS (SELECT 1 FROM dbo.room_refresh_inventory_mappings WHERE room_type_id = @deluxeRoomTypeId AND inventory_item_id = @towelItemId)
        INSERT INTO dbo.room_refresh_inventory_mappings (room_type_id, inventory_item_id, quantity_per_refresh)
        VALUES (@deluxeRoomTypeId, @towelItemId, 2);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.inventory_transactions WHERE source_type = 'SEED' AND source_id = @waterItemId)
    INSERT INTO dbo.inventory_transactions (inventory_item_id, transaction_type, quantity, description, source_type, source_id, created_by, created_at)
    VALUES (@waterItemId, 'IN', 100, N'Tồn đầu kỳ seed', 'SEED', @waterItemId, @managerId, @now);
IF NOT EXISTS (SELECT 1 FROM dbo.inventory_transactions WHERE source_type = 'SEED' AND source_id = @towelItemId)
    INSERT INTO dbo.inventory_transactions (inventory_item_id, transaction_type, quantity, description, source_type, source_id, created_by, created_at)
    VALUES (@towelItemId, 'IN', 30, N'Tồn đầu kỳ seed', 'SEED', @towelItemId, @managerId, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.hotel_fund_settings)
    INSERT INTO dbo.hotel_fund_settings (
        opening_balance, opening_cash_balance, opening_transfer_balance, opening_card_balance,
        configured_by, configured_at
    )
    VALUES (50000000, 20000000, 25000000, 5000000, @managerId, @now);

DECLARE @room102Id bigint = (SELECT room_id FROM dbo.rooms WHERE room_number = '102');

IF NOT EXISTS (SELECT 1 FROM dbo.bookings WHERE booking_reference = 'BK-SEED-001')
BEGIN
    INSERT INTO dbo.bookings (
        guest_first_name, guest_last_name, guest_phone, guest_email, guest_id, discount_amount,
        check_in_date, check_out_date, num_adults, total_rooms, num_children, special_requests,
        booking_reference, deposit_status, status, total_amount,
        room_subtotal, service_subtotal, service_charge_total, vat_total, grand_total,
        amount_calculated_at,
        created_by, actual_checkout_at, is_deleted, created_at, updated_at
    )
    VALUES (
        N'Guest', N'Test', '0900000004', N'guest@test.com', @guestId, 0,
        CAST(GETDATE() AS date), DATEADD(day, 1, CAST(GETDATE() AS date)), 2, 1, 0, N'Seed booking',
        'BK-SEED-001', 'PAID', 'CHECKED_IN', 963900,
        800000, 50000, 42500, 71400, 963900,
        @now,
        @receptionistId, NULL, 0, @now, @now
    );
END;

DECLARE @bookingId bigint = (SELECT booking_id FROM dbo.bookings WHERE booking_reference = 'BK-SEED-001');

IF NOT EXISTS (SELECT 1 FROM dbo.booking_details WHERE booking_id = @bookingId AND room_id = @room102Id)
    INSERT INTO dbo.booking_details (
        booking_id, room_type_id, room_id, check_in_date, check_out_date, price_per_night,
        num_nights, subtotal, service_charge_rate, service_charge_amount, vat_rate, vat_amount, total_amount,
        room_code, room_code_expires_at, view_type, num_adults, num_children, child_ages,
        extra_bed_count, extra_bed_price, extra_bed_total
    )
    VALUES (
        @bookingId, @standardRoomTypeId, @room102Id, CAST(GETDATE() AS date), DATEADD(day, 1, CAST(GETDATE() AS date)),
        800000, 1, 800000, 5.00, 40000, 8.00, 67200, 907200,
        NULL, NULL, 'CITY', 2, 0, NULL,
        0, 0, 0
    );

IF NOT EXISTS (SELECT 1 FROM dbo.folio_items WHERE booking_id = @bookingId AND description = N'Nước suối minibar')
    INSERT INTO dbo.folio_items (
        booking_id, service_id, description, item_type, amount,
        base_amount, service_charge_rate, service_charge_amount, vat_rate, vat_amount, total_amount, price_display_mode,
        quantity, unit_price,
        posted_at, posted_by, adjustment_reason, is_voided, voided_by, voided_at, voided_reason
    )
    VALUES (
        @bookingId, @waterServiceId, N'Nước suối minibar', 'FOOD', 56700,
        50000, 5.00, 2500, 8.00, 4200, 56700, 'PLUS_PLUS',
        2, 25000, @now, @receptionistId, NULL, 0, NULL, NULL, NULL
    );

IF NOT EXISTS (SELECT 1 FROM dbo.payments WHERE transaction_ref = 'PAY-SEED-001')
    INSERT INTO dbo.payments (
        booking_id, payment_type, method, amount, status, transaction_ref, processed_by, paid_at, created_at, original_payment_id
    )
    VALUES (@bookingId, 'DEPOSIT', 'CASH', 300000, 'SUCCESS', 'PAY-SEED-001', @receptionistId, @now, @now, NULL);

DECLARE @paymentId bigint = (SELECT payment_id FROM dbo.payments WHERE transaction_ref = 'PAY-SEED-001');

IF NOT EXISTS (SELECT 1 FROM dbo.cash_transactions WHERE transaction_code = 'CT-SEED-PAYMENT')
    INSERT INTO dbo.cash_transactions (
        transaction_code, document_code, transaction_type, category, amount, fund_method,
        description, source_type, source_id, created_by, created_at
    )
    VALUES ('CT-SEED-PAYMENT', 'PT-SEED-PAYMENT', 'INCOME', 'DEPOSIT', 300000, 'CASH',
            N'Đặt cọc booking BK-SEED-001', 'PAYMENT', @paymentId, @receptionistId, @now);

IF NOT EXISTS (SELECT 1 FROM dbo.cash_transactions WHERE transaction_code = 'CT-SEED-MANUAL-IN')
    INSERT INTO dbo.cash_transactions (
        transaction_code, document_code, transaction_type, category, amount, fund_method,
        description, source_type, source_id, created_by, created_at
    )
    VALUES ('CT-SEED-MANUAL-IN', 'PT-SEED-MANUAL-IN', 'INCOME', 'MANUAL_INCOME', 1500000, 'TRANSFER',
            N'Thu bổ sung quỹ test', 'MANUAL', NULL, @managerId, DATEADD(minute, 1, @now));

IF NOT EXISTS (SELECT 1 FROM dbo.cash_transactions WHERE transaction_code = 'CT-SEED-MANUAL-OUT')
    INSERT INTO dbo.cash_transactions (
        transaction_code, document_code, transaction_type, category, amount, fund_method,
        description, source_type, source_id, created_by, created_at
    )
    VALUES ('CT-SEED-MANUAL-OUT', 'PC-SEED-MANUAL-OUT', 'EXPENSE', 'MANUAL_EXPENSE', 500000, 'CASH',
            N'Chi vận hành test', 'MANUAL', NULL, @managerId, DATEADD(minute, 2, @now));

COMMIT TRANSACTION;

SELECT 'Seed completed' AS message;
SELECT role_id, role_name FROM dbo.roles ORDER BY role_id;
SELECT email, user_type, is_active FROM dbo.users WHERE email IN (N'manager@test.com', N'hoteladmin@test.com', N'receptionist@test.com', N'guest@test.com');
GO
