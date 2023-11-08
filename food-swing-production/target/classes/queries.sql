-- Foreign key truncate all
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `meal_suggestion_details`;
-- Starting from id 1 in table after del
ALTER TABLE `meal_suggestion_details` AUTO_INCREMENT = 1;


-- Add a new column 'uom_id' to the 'vessel' table
ALTER TABLE vessel
ADD uom_id INT;


-- Alter the 'status' column of the 'vessel' table
ALTER TABLE vessel
CHANGE COLUMN `status` `status` VARCHAR(40) NOT NULL ;


-- Alter the 'capacity' column of the 'vessel' table
ALTER TABLE vessel
CHANGE COLUMN `capacity` `capacity` DECIMAL(6,3) NOT NULL ;


-- Add a new column 'preparation_type' to the 'recipe_master' table
ALTER TABLE recipe_master
ADD preparation_type VARCHAR(1);


--Default value set A
ALTER TABLE recipe_master
ALTER COLUMN preparation_type SET DEFAULT 'A';


--Alter the 'meal_start_time' colum to default null
ALTER TABLE order_master
CHANGE COLUMN `meal_start_time` `meal_start_time` TIMESTAMP NULL DEFAULT NULL ;


-- Add a new column 'indent_sent' to the 'order_master' table
ALTER TABLE order_master
ADD indent_sent VARCHAR(1);


--Default value set Y
ALTER TABLE order_master
ALTER COLUMN indent_sent SET DEFAULT 'Y';


-- Add a new column 'category_id' to the 'item' table
ALTER TABLE item
ADD COLUMN `category_id` INT NULL AFTER `uom_id`;


-- Add a new column 'gram' to the 'item' table
ALTER TABLE item
ADD COLUMN `gram` DECIMAL(10,4) NULL AFTER `category_id`;


--Alter the 'status' column to default null
ALTER TABLE vessel
CHANGE COLUMN `status` `status` VARCHAR(40) NULL DEFAULT NULL ;


DROP TABLE cka_gandhi_d_recipe_xl;

DROP TABLE cka_meal_list_d_recipe_xl;

DROP TABLE ingredient_price_history;

DROP TABLE ingredients_item_mapping;

DROP TABLE ingredients_details;

DROP TABLE menu_details;

DROP TABLE menu_master;

DROP TABLE production_and_overall_recipe_indent_xl;

DROP TABLE production_ingredients;

DROP TABLE production_plan;

DROP TABLE vendor_master;

DROP TABLE plan;

DROP TABLE plan_detail;


--New table created for mealCategoryMapping
CREATE TABLE meal_category_mapping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    meal_id TINYINT,
    category_id TINYINT,
    created_by TINYINT,
    created_time TIMESTAMP,
    updated_by TINYINT,
    updated_time TIMESTAMP,
    is_active TINYINT
);

-- Add a new column 'weight' to the 'item' table
ALTER TABLE item
ADD COLUMN `weight` DECIMAL(10,4);

--Add a new table for company and default cost mapping
CREATE TABLE company_default_cost_config (
  `id` INT NOT NULL AUTO_INCREMENT,
  `company_id` MEDIUMINT NOT NULL,
  `meal_id` SMALLINT NOT NULL,
  `default_cost` DECIMAL(10,4) NOT NULL,
  `created_by` MEDIUMINT NULL,
  `created_time` TIMESTAMP NULL,
  `updated_by` MEDIUMINT NULL,
  `updated_time` TIMESTAMP NULL,
  `is_active` TINYINT NULL,
  PRIMARY KEY (`id`)
  );


  --Add a new table for company and company meal cost mapping
  CREATE TABLE company_meal_cost_mapping (
      id INT AUTO_INCREMENT PRIMARY KEY,
      company_id TINYINT,
      meal_id TINYINT,
      day TINYINT,
      cost DECIMAL(6,3),
      created_by TINYINT,
      created_time TIMESTAMP,
      updated_by TINYINT,
      updated_time TIMESTAMP,
      is_active TINYINT
  );

  CREATE TABLE company_meal_cost_mapping (
    `id` INT NOT NULL AUTO_INCREMENT,
    `company_id` MEDIUMINT NOT NULL,
    `meal_id` SMALLINT NOT NULL,
    `day` SMALLINT NOT NULL,
    `cost` DECIMAL(10,4) NOT NULL,
    `created_by` MEDIUMINT NULL,
    `created_time` TIMESTAMP NULL,
    `updated_by` MEDIUMINT NULL,
    `updated_time` TIMESTAMP NULL,
    `is_active` TINYINT NULL,
    PRIMARY KEY (`id`)
    );


-- Add a new column 'vessel_type' to the 'vessel' table
ALTER TABLE vessel
ADD COLUMN `vessel_type` VARCHAR(45) NULL AFTER `capacity`;


--New table created for vesselItemMapping
CREATE TABLE vessel_item_mapping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vessel_id TINYINT,
    item_id TINYINT,
    max_capacity DECIMAL(6,3),
    created_by TINYINT,
    created_time TIMESTAMP,
    updated_by TINYINT,
    updated_time TIMESTAMP,
    is_active TINYINT
);


--Alter status column to int
ALTER TABLE vessel
CHANGE COLUMN `status` `status` INT NULL DEFAULT NULL ;


--New Column Count
ALTER TABLE meal_category_mapping
ADD COLUMN `count` MEDIUMINT NOT NULL AFTER `is_active`;


-- Add a new column 'status' to the 'order' table
ALTER TABLE order_master
ADD COLUMN `status` int NULL AFTER `meal_start_time`;

---Default UOM count
INSERT INTO uom_master (`uom_id`, `name`, `created_by`, `created_time`, `is_active`)
VALUES ('1', 'Count', '1', '2023-10-04 15:30:45', '1');

--Default Medium
INSERT INTO medium_master (`medium_id`, `medium_name`, `created_by`, `created_time`, `is_active`)
 VALUES ('1', 'Direct', '1', '2023-10-04) 15:30:45', '1');
 INSERT INTO medium_master (`medium_id`, `medium_name`, `created_by`, `created_time`, `is_active`)
  VALUES ('2', 'Grinding', '1', '2023-10-04) 15:30:45', '1');
   INSERT INTO medium_master (`medium_id`, `medium_name`, `created_by`, `created_time`, `is_active`)
    VALUES ('3', 'Mise En Place', '1', '2023-10-04) 15:30:45', '1');


    --New table created for mealProductionPlan
    CREATE TABLE meal_production_plan (
        id INT AUTO_INCREMENT PRIMARY KEY,
        plan_id TINYINT,
        meal_suggestion_id TINYINT,
        item_id TINYINT,
        vessel_id TINYINT,
        recipe_id TINYINT
    );

    -- Add a new column 'stock' to the 'ingredients' table
    ALTER TABLE ingredients
    ADD COLUMN `stock` DECIMAL(10,3) NULL AFTER `cost`;

--New table created for stock_audit
CREATE TABLE stock_audit (
        id INT AUTO_INCREMENT PRIMARY KEY,
                ingredient_id TINYINT,
                ingredient_qty DECIMAL(10,3),
                used_qty DECIMAL(10,3),
                wastage_qty DECIMAL(10,3),
                price DECIMAL(10,3),
                created_by TINYINT,
                created_time TIMESTAMP
    );

    --Default set 0 in stock
    ALTER TABLE ingredients
    CHANGE COLUMN `stock` `stock` DECIMAL(10,3) NULL DEFAULT 0 ;
