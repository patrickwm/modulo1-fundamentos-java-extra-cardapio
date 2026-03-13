package mx.florinda.cardapio.database;

import mx.florinda.cardapio.ItemCardapio;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLDatabase implements Database {

    @Override
    public List<ItemCardapio> listaItensCardapio() {

        var itensCardapio = new ArrayList<ItemCardapio>();

        var sql = "SELECT id, nome, descricao, categoria, preco, preco_promocional FROM item_cardapio";
        try (var conn =
                     DriverManager.getConnection("jdbc:mysql://localhost:3306/cardapio", "root", "senha123");
            var ps = conn.prepareStatement(sql);
            var rs = ps.executeQuery()){

            while (rs.next()) {
                var id = rs.getLong("id");
                var nome = rs.getString("nome");
                var descricao = rs.getString("descricao");
                var categoriaStr = rs.getString("categoria");
                var preco = rs.getBigDecimal("preco");
                var precoPromocional = rs.getBigDecimal("preco_promocional");

                var categoria = ItemCardapio.CategoriaCardapio.valueOf(categoriaStr);

                var itemCardapio = new ItemCardapio(id, nome, descricao, categoria, preco, precoPromocional);

                itensCardapio.add(itemCardapio);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return itensCardapio;
    }

    @Override
    public int totalItensCardapio() {
        var sql = "SELECT count(*) FROM item_cardapio";
        try (var conn =
                     DriverManager.getConnection("jdbc:mysql://localhost:3306/cardapio", "root", "senha123");
            var ps = conn.prepareStatement(sql);
            var rs = ps.executeQuery()){

            var count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            return count;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void adicionaItemCardapio(ItemCardapio item) {
        var sql = "INSERT INTO item_cardapio (id, nome, descricao, categoria, preco, preco_promocional) VALUES (?, ?, ?, ?, ?, ?)";
        try (var conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cardapio", "root", "senha123");
             var ps = conn.prepareStatement(sql)) {
                ps.setLong(1, item.id());
                ps.setString(2, item.nome());
                ps.setString(3, item.descricao());
                ps.setString(4, item.categoria().name());
                ps.setBigDecimal(5, item.preco());
                ps.setBigDecimal(6, item.precoPromocional());

                ps.execute();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ItemCardapio> itemCardapioPorId(Long id) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean removeItemCardapio(Long id) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean alteraPrecoItemCardapio(Long id, BigDecimal novoPreco) {
        throw new UnsupportedOperationException("TODO");
    }


}
