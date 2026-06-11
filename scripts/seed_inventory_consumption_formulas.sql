USE [Vhotel_HN_test];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @now datetime2 = SYSDATETIME();

IF NOT EXISTS (SELECT 1 FROM dbo.service_categories WHERE type = 'FOOD' AND is_deleted = 0)
    INSERT INTO dbo.service_categories (name, type, description, is_deleted)
    VALUES (N'Ẩm thực', 'FOOD', N'Dịch vụ đồ ăn, đồ uống và minibar', 0);

IF NOT EXISTS (SELECT 1 FROM dbo.service_categories WHERE type = 'SPA' AND is_deleted = 0)
    INSERT INTO dbo.service_categories (name, type, description, is_deleted)
    VALUES (N'Spa', 'SPA', N'Dịch vụ chăm sóc sức khỏe và thư giãn', 0);

DECLARE @foodCategoryId bigint = (
    SELECT TOP 1 service_category_id FROM dbo.service_categories WHERE type = 'FOOD' AND is_deleted = 0 ORDER BY service_category_id
);
DECLARE @spaCategoryId bigint = (
    SELECT TOP 1 service_category_id FROM dbo.service_categories WHERE type = 'SPA' AND is_deleted = 0 ORDER BY service_category_id
);

DECLARE @items TABLE (
    code varchar(30),
    name nvarchar(150),
    category nvarchar(100),
    unit nvarchar(30),
    opening_quantity decimal(12,2),
    minimum_quantity decimal(12,2),
    unit_cost decimal(12,2)
);

INSERT INTO @items (code, name, category, unit, opening_quantity, minimum_quantity, unit_cost)
VALUES
('IT-CONS-SHAMPOO', N'Dầu gội', N'Đồ dùng tiêu hao', N'chai', 100, 20, 15000),
('IT-CONS-BODYWASH', N'Sữa tắm', N'Đồ dùng tiêu hao', N'chai', 100, 20, 15000),
('IT-CONS-SOAP', N'Xà bông cục', N'Đồ dùng tiêu hao', N'cục', 200, 40, 5000),
('IT-CONS-TOOTHBRUSH', N'Bàn chải', N'Đồ dùng tiêu hao', N'cái', 200, 40, 4000),
('IT-CONS-TOOTHPASTE', N'Kem đánh răng', N'Đồ dùng tiêu hao', N'tuýp', 150, 30, 8000),
('IT-CONS-COMB', N'Lược', N'Đồ dùng tiêu hao', N'cái', 150, 30, 3000),
('IT-CONS-RAZOR', N'Dao cạo râu', N'Đồ dùng tiêu hao', N'cái', 100, 20, 6000),
('IT-CONS-TEA', N'Trà túi lọc', N'Đồ dùng tiêu hao', N'gói', 300, 60, 2000),
('IT-CONS-COFFEE', N'Cà phê hòa tan', N'Đồ dùng tiêu hao', N'gói', 300, 60, 2500),
('IT-CONS-WATER', N'Nước lọc', N'Đồ dùng tiêu hao', N'chai', 300, 60, 5000),
('IT-CONS-SLIPPERS', N'Dép đi trong nhà', N'Đồ dùng tiêu hao', N'đôi', 120, 24, 12000),
('IT-MINI-SOFTDRINK', N'Nước ngọt', N'Hàng hóa Mini-bar', N'lon', 120, 24, 9000),
('IT-MINI-BEER', N'Bia', N'Hàng hóa Mini-bar', N'lon', 120, 24, 15000),
('IT-MINI-WINE', N'Rượu vang', N'Hàng hóa Mini-bar', N'chai', 30, 6, 180000),
('IT-MINI-SNACK', N'Snack', N'Hàng hóa Mini-bar', N'gói', 120, 24, 12000),
('IT-SPA-CREAM', N'Kem dưỡng', N'Dịch vụ spa', N'hộp', 30, 6, 120000),
('IT-SPA-OIL', N'Tinh dầu', N'Dịch vụ spa', N'chai', 40, 8, 90000),
('IT-SPA-TOWEL', N'Khăn spa', N'Dịch vụ spa', N'cái', 80, 16, 45000),
('IT-FOOD-BEEF', N'Thịt bò', N'Món ăn', N'kg', 20, 5, 250000),
('IT-FOOD-CHICKEN', N'Thịt gà', N'Món ăn', N'kg', 25, 5, 90000),
('IT-FOOD-PORK', N'Thịt lợn', N'Món ăn', N'kg', 25, 5, 120000),
('IT-FOOD-TOMATO', N'Cà chua', N'Món ăn', N'kg', 20, 5, 25000),
('IT-FOOD-LETTUCE', N'Xà lách', N'Món ăn', N'kg', 15, 3, 30000),
('IT-FOOD-PURPLE-CABBAGE', N'Bắp cải tím', N'Món ăn', N'kg', 15, 3, 35000),
('IT-FOOD-CARROT', N'Cà rốt', N'Món ăn', N'kg', 20, 5, 22000),
('IT-FOOD-EGG', N'Trứng', N'Món ăn', N'quả', 100, 20, 3500);

INSERT INTO dbo.inventory_items (
    item_code, name, category, unit, opening_quantity, current_quantity,
    minimum_quantity, unit_cost, is_deleted, created_at, updated_at
)
SELECT i.code, i.name, i.category, i.unit, i.opening_quantity, i.opening_quantity,
       i.minimum_quantity, i.unit_cost, 0, @now, @now
FROM @items i
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.inventory_items existing
    WHERE existing.is_deleted = 0
      AND (existing.item_code = i.code OR existing.name = i.name)
);

DECLARE @services TABLE (
    name nvarchar(200),
    category_id bigint,
    description nvarchar(300),
    price decimal(12,2)
);

INSERT INTO @services (name, category_id, description, price)
VALUES
(N'Nước lọc minibar', @foodCategoryId, N'Bán 1 chai nước lọc trong minibar', 25000),
(N'Nước ngọt minibar', @foodCategoryId, N'Bán 1 lon nước ngọt trong minibar', 35000),
(N'Bia minibar', @foodCategoryId, N'Bán 1 lon bia trong minibar', 55000),
(N'Rượu vang minibar', @foodCategoryId, N'Bán 1 chai rượu vang trong minibar', 350000),
(N'Snack minibar', @foodCategoryId, N'Bán 1 gói snack trong minibar', 45000),
(N'Massage thư giãn', @spaCategoryId, N'Dịch vụ massage 60 phút', 450000),
(N'Chăm sóc da spa', @spaCategoryId, N'Dịch vụ chăm sóc da cơ bản', 500000),
(N'Salad trứng', @foodCategoryId, N'Salad rau củ kèm trứng', 120000),
(N'Bò áp chảo', @foodCategoryId, N'Món bò áp chảo', 280000),
(N'Gà nướng', @foodCategoryId, N'Món gà nướng', 180000),
(N'Cơm thịt lợn', @foodCategoryId, N'Món cơm thịt lợn', 150000);

INSERT INTO dbo.services (category_id, name, description, price, is_available, is_deleted, created_at, updated_at)
SELECT s.category_id, s.name, s.description, s.price, 1, 0, @now, @now
FROM @services s
WHERE NOT EXISTS (SELECT 1 FROM dbo.services existing WHERE existing.name = s.name AND existing.is_deleted = 0);

DECLARE @serviceFormulas TABLE (
    service_name nvarchar(200),
    item_name nvarchar(150),
    quantity_per_use decimal(12,2)
);

INSERT INTO @serviceFormulas (service_name, item_name, quantity_per_use)
VALUES
(N'Nước lọc minibar', N'Nước lọc', 1),
(N'Nước ngọt minibar', N'Nước ngọt', 1),
(N'Bia minibar', N'Bia', 1),
(N'Rượu vang minibar', N'Rượu vang', 1),
(N'Snack minibar', N'Snack', 1),
(N'Massage thư giãn', N'Tinh dầu', 0.05),
(N'Massage thư giãn', N'Khăn spa', 1),
(N'Chăm sóc da spa', N'Kem dưỡng', 0.03),
(N'Chăm sóc da spa', N'Tinh dầu', 0.03),
(N'Chăm sóc da spa', N'Khăn spa', 1),
(N'Salad trứng', N'Cà chua', 0.10),
(N'Salad trứng', N'Xà lách', 0.08),
(N'Salad trứng', N'Bắp cải tím', 0.05),
(N'Salad trứng', N'Cà rốt', 0.05),
(N'Salad trứng', N'Trứng', 1),
(N'Bò áp chảo', N'Thịt bò', 0.25),
(N'Bò áp chảo', N'Cà chua', 0.05),
(N'Bò áp chảo', N'Xà lách', 0.05),
(N'Gà nướng', N'Thịt gà', 0.25),
(N'Gà nướng', N'Cà rốt', 0.05),
(N'Gà nướng', N'Xà lách', 0.05),
(N'Cơm thịt lợn', N'Thịt lợn', 0.25),
(N'Cơm thịt lợn', N'Cà chua', 0.05),
(N'Cơm thịt lợn', N'Cà rốt', 0.05);

INSERT INTO dbo.service_inventory_mappings (service_id, inventory_item_id, quantity_per_use)
SELECT s.service_id, i.inventory_item_id, f.quantity_per_use
FROM @serviceFormulas f
JOIN dbo.services s ON s.name = f.service_name AND s.is_deleted = 0
JOIN dbo.inventory_items i ON i.name = f.item_name AND i.is_deleted = 0
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.service_inventory_mappings existing
    WHERE existing.service_id = s.service_id
      AND existing.inventory_item_id = i.inventory_item_id
);

IF OBJECT_ID(N'dbo.room_refresh_inventory_mappings', N'U') IS NOT NULL
BEGIN
    DECLARE @refreshFormulas TABLE (
        room_type_name nvarchar(100),
        item_name nvarchar(150),
        quantity_per_refresh decimal(12,2)
    );

    INSERT INTO @refreshFormulas (room_type_name, item_name, quantity_per_refresh)
    VALUES
    (N'STANDARD', N'Dầu gội', 1), (N'STANDARD', N'Sữa tắm', 1), (N'STANDARD', N'Xà bông cục', 1),
    (N'STANDARD', N'Bàn chải', 2), (N'STANDARD', N'Kem đánh răng', 1), (N'STANDARD', N'Lược', 1),
    (N'STANDARD', N'Dao cạo râu', 1), (N'STANDARD', N'Trà túi lọc', 2), (N'STANDARD', N'Cà phê hòa tan', 2),
    (N'STANDARD', N'Nước lọc', 2), (N'STANDARD', N'Dép đi trong nhà', 2),

    (N'DELUXE', N'Dầu gội', 2), (N'DELUXE', N'Sữa tắm', 2), (N'DELUXE', N'Xà bông cục', 2),
    (N'DELUXE', N'Bàn chải', 2), (N'DELUXE', N'Kem đánh răng', 1), (N'DELUXE', N'Lược', 2),
    (N'DELUXE', N'Dao cạo râu', 2), (N'DELUXE', N'Trà túi lọc', 4), (N'DELUXE', N'Cà phê hòa tan', 4),
    (N'DELUXE', N'Nước lọc', 4), (N'DELUXE', N'Dép đi trong nhà', 2),

    (N'SUITE', N'Dầu gội', 4), (N'SUITE', N'Sữa tắm', 4), (N'SUITE', N'Xà bông cục', 4),
    (N'SUITE', N'Bàn chải', 4), (N'SUITE', N'Kem đánh răng', 2), (N'SUITE', N'Lược', 4),
    (N'SUITE', N'Dao cạo râu', 4), (N'SUITE', N'Trà túi lọc', 6), (N'SUITE', N'Cà phê hòa tan', 6),
    (N'SUITE', N'Nước lọc', 6), (N'SUITE', N'Dép đi trong nhà', 4);

    INSERT INTO dbo.room_refresh_inventory_mappings (room_type_id, inventory_item_id, quantity_per_refresh)
    SELECT rt.room_type_id, i.inventory_item_id, f.quantity_per_refresh
    FROM @refreshFormulas f
    JOIN dbo.room_types rt ON rt.name = f.room_type_name AND rt.is_deleted = 0
    JOIN dbo.inventory_items i ON i.name = f.item_name AND i.is_deleted = 0
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.room_refresh_inventory_mappings existing
        WHERE existing.room_type_id = rt.room_type_id
          AND existing.inventory_item_id = i.inventory_item_id
    );
END;

COMMIT TRANSACTION;

SELECT 'Inventory consumption formulas seeded' AS message;
SELECT s.name AS service_name, i.name AS item_name, m.quantity_per_use
FROM dbo.service_inventory_mappings m
JOIN dbo.services s ON s.service_id = m.service_id
JOIN dbo.inventory_items i ON i.inventory_item_id = m.inventory_item_id
ORDER BY s.name, i.name;

IF OBJECT_ID(N'dbo.room_refresh_inventory_mappings', N'U') IS NOT NULL
BEGIN
    SELECT rt.name AS room_type_name, i.name AS item_name, m.quantity_per_refresh
    FROM dbo.room_refresh_inventory_mappings m
    JOIN dbo.room_types rt ON rt.room_type_id = m.room_type_id
    JOIN dbo.inventory_items i ON i.inventory_item_id = m.inventory_item_id
    ORDER BY rt.name, i.name;
END;
GO
