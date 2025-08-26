package mx.florinda.cardapio;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLDatabase implements Database {

    @Override
    public List<ItemCardapio> listaItensCardapio() {

        List<ItemCardapio> itensCardapio = new ArrayList<>();

        String sql = "SELECT id, nome, descricao, categoria, preco, preco_promocional FROM item_cardapio";
        try (Connection conn =
                     DriverManager.getConnection("jdbc:mysql://localhost:3306/cardapio", "root", "senha123");
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){

            while (rs.next()) {
                long id = rs.getLong("id");
                String nome = rs.getString("nome");
                String descricao = rs.getString("descricao");
                String categoriaStr = rs.getString("categoria");
                BigDecimal preco = rs.getBigDecimal("preco");
                BigDecimal precoPromocional = rs.getBigDecimal("preco_promocional");

                ItemCardapio.CategoriaCardapio categoria = ItemCardapio.CategoriaCardapio.valueOf(categoriaStr);

                ItemCardapio itemCardapio = new ItemCardapio(id, nome, descricao, categoria, preco, precoPromocional);

                itensCardapio.add(itemCardapio);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return itensCardapio;
    }

    @Override
    public int totalItensCardapio() {
        String sql = "SELECT count(*) FROM item_cardapio";
        try (Connection conn =
                     DriverManager.getConnection("jdbc:mysql://localhost:3306/cardapio", "root", "senha123");
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){

            int count = 0;
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
        String sql = "INSERT INTO item_cardapio (id, nome, descricao, categoria, preco, preco_promocional) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cardapio", "root", "senha123");
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
