-- ========================================
-- MÓDULO FLOTAS - FK de trazabilidad hacia el vehículo maestro
-- Migration 019
-- ========================================
-- Enlaza las tablas de trazabilidad (module_vehicle_*) al vehículo maestro
-- (module_fleet_vehicles) por yango_car_id, con ON DELETE RESTRICT.
--
-- Se usa NOT VALID para no fallar si existieran filas legacy huérfanas
-- (yango_car_id sin vehículo cacheado aún). La integridad queda garantizada
-- para todas las filas nuevas. Tras el primer sync se puede validar con:
--   ALTER TABLE module_vehicle_documents   VALIDATE CONSTRAINT fk_docs_vehicle;
--   ALTER TABLE module_vehicle_maintenance VALIDATE CONSTRAINT fk_maint_vehicle;
--   ALTER TABLE module_vehicle_mileage     VALIDATE CONSTRAINT fk_mileage_vehicle;
--   ALTER TABLE module_vehicle_incidents   VALIDATE CONSTRAINT fk_incidents_vehicle;

ALTER TABLE module_vehicle_documents
    ADD CONSTRAINT fk_docs_vehicle
    FOREIGN KEY (yango_car_id) REFERENCES module_fleet_vehicles (yango_car_id)
    ON DELETE RESTRICT NOT VALID;

ALTER TABLE module_vehicle_maintenance
    ADD CONSTRAINT fk_maint_vehicle
    FOREIGN KEY (yango_car_id) REFERENCES module_fleet_vehicles (yango_car_id)
    ON DELETE RESTRICT NOT VALID;

ALTER TABLE module_vehicle_mileage
    ADD CONSTRAINT fk_mileage_vehicle
    FOREIGN KEY (yango_car_id) REFERENCES module_fleet_vehicles (yango_car_id)
    ON DELETE RESTRICT NOT VALID;

ALTER TABLE module_vehicle_incidents
    ADD CONSTRAINT fk_incidents_vehicle
    FOREIGN KEY (yango_car_id) REFERENCES module_fleet_vehicles (yango_car_id)
    ON DELETE RESTRICT NOT VALID;
