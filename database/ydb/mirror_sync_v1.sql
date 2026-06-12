-- Mirror sync v1 schema.
-- Все даты и timestamps пока храним как ISO-8601 Utf8, чтобы не потерять
-- текущее timezone-neutral поведение локальных LocalDate / LocalDateTime.

CREATE TABLE IF NOT EXISTS `vendor` (
    sync_id Utf8,
    display_name Utf8,
    comment Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `document` (
    sync_id Utf8,
    display_name Utf8,
    type Utf8,
    created_at Utf8,
    comment Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `declaration` (
    sync_id Utf8,
    display_name Utf8,
    created_at Utf8,
    vendor_sync_id Utf8,
    vendor_name Utf8,
    born_date Utf8,
    best_before Utf8,
    observe_from_notification Bool,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `product` (
    sync_id Utf8,
    type Utf8,
    display_name Utf8,
    second_name Utf8,
    created_at Utf8,
    comment Utf8,
    price_for_sale Int64,
    shelf_life_days Int32,
    rec_nds Int32,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `product_specification` (
    sync_id Utf8,
    product_sync_id Utf8,
    dosage Utf8,
    composition Utf8,
    shelf_life_text Utf8,
    storage_conditions Utf8,
    appearance Utf8,
    color Utf8,
    smell Utf8,
    taste Utf8,
    physical_chemical_indicators Utf8,
    microbiological_indicators Utf8,
    toxic_elements Utf8,
    allergens Utf8,
    gmo_info Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `safety_stock` (
    sync_id Utf8,
    product_sync_id Utf8,
    reorder_point Int64,
    order_quantity Int64,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `composition` (
    sync_id Utf8,
    parent_sync_id Utf8,
    product_sync_id Utf8,
    count Int64,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `product_declaration` (
    sync_id Utf8,
    product_sync_id Utf8,
    declaration_sync_id Utf8,
    is_product_in_declaration Bool,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `batch` (
    sync_id Utf8,
    product_sync_id Utf8,
    date_born Utf8,
    declaration_sync_id Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `batch_cost_price` (
    sync_id Utf8,
    batch_sync_id Utf8,
    cost_price_per_unit Int64,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `batch_movement` (
    sync_id Utf8,
    batch_sync_id Utf8,
    movement_type Utf8,
    count Int64,
    transaction_sync_id Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `transact` (
    sync_id Utf8,
    transaction_type Utf8,
    created_at Utf8,
    comment Utf8,
    is_completed Bool,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `buy` (
    sync_id Utf8,
    transaction_sync_id Utf8,
    movement_sync_id Utf8,
    price Int64,
    comment Utf8,
    nds_percent Int32,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `sale` (
    sync_id Utf8,
    transaction_sync_id Utf8,
    movement_sync_id Utf8,
    price Int64,
    comment Utf8,
    client_sync_id Utf8,
    nds_percent Int32,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `reminder` (
    sync_id Utf8,
    transaction_sync_id Utf8,
    text Utf8,
    reminder_date_time Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `expense` (
    sync_id Utf8,
    transaction_sync_id Utf8,
    expense_type Utf8,
    amount Int64,
    expense_date_time Utf8,
    comment Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `experiment` (
    sync_id Utf8,
    title Utf8,
    idea_description Utf8,
    is_archived Bool,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `experiment_entry` (
    sync_id Utf8,
    experiment_sync_id Utf8,
    entry_date Utf8,
    content Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `experiment_reminder` (
    sync_id Utf8,
    experiment_sync_id Utf8,
    text Utf8,
    reminder_date_time Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);

CREATE TABLE IF NOT EXISTS `file` (
    sync_id Utf8,
    owner_type Utf8,
    owner_sync_id Utf8,
    display_name Utf8,
    path Utf8,
    remote_object_key Utf8,
    remote_storage_provider Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);
