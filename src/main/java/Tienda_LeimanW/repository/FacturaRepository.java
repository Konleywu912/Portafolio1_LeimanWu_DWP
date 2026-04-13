/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tienda_LeimanW.repository;

import Tienda_LeimanW.domain.Factura;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Integer>{
    // Consulta para cargar la Factura, su Usuario, sus Ventas y los Productos de esas ventas.
    @Query("SELECT f FROM Factura f " +
           "LEFT JOIN FETCH f.usuario u " +       // Carga inmediata del Usuario
           "LEFT JOIN FETCH f.ventas v " +        // Carga inmediata de la lista de Ventas
           "LEFT JOIN FETCH v.producto p " +      // Carga inmediata del Producto en cada Venta
           "WHERE f.idFactura = :idFactura")
    Optional<Factura> findByIdFacturaConDetalle(@Param("idFactura") Integer idFactura);
}
