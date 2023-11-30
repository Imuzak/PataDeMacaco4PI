package com.patademacaco.persistencia;

import com.patademacaco.enumeracao.Status;
import com.patademacaco.enumeracao.Tipo;
import com.patademacaco.ferramentas.ConexaoBD;
import com.patademacaco.modelo.Categoria;
import com.patademacaco.modelo.Denuncia;
import com.patademacaco.modelo.Endereco;
import com.patademacaco.modelo.Fotos;
import com.patademacaco.modelo.Municipio;
import com.patademacaco.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DenunciaDao implements IDenunciaDao{
    
    //ATRIBUTOS DA CLASSE CONECTADA
    private Connection conexao = null;

    //METODOS 
    public DenunciaDao() throws Exception {
        conexao = ConexaoBD.getConexao();
    }
    
    @Override
    public ArrayList<Municipio> ListarMunicipio() throws Exception {
        ArrayList<Municipio> listaMunicipios = new ArrayList<>();
        String sql = "SELECT * FROM municipios ";
        try{
            Statement statement = conexao.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                Municipio municipio = new Municipio();
                municipio.setId(Integer.parseInt(rs.getString("idMunicipio")));
                municipio.setNome(rs.getString("nome"));
                municipio.setUf(rs.getString("uf"));
                municipio.setCodIBGE(Integer.parseInt(rs.getString("codigoIBGE")));
                listaMunicipios.add(municipio);
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return listaMunicipios;
    }
    
    @Override
    public String Cadastrar(Denuncia objeto) throws Exception {
        
        try {
            String sqlFotos = "INSERT INTO fotos (url1, url2, url3, url4) Values (?, ?, ?, ?) RETURNING idFotos";
            String sql = "INSERT INTO denuncia(cpfDenunciante, cpfAnalista, idMunicipio, cep, logradouro, bairro, pontoDeReferencia, descricao, idCategoria, idSubcategoria, data, autorCrime, parecer, status, idFotos)";
            sql+= " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING idDenuncia";
            PreparedStatement preparedStatement = conexao.prepareStatement(sqlFotos);
            int idFotos = preparedStatement.executeUpdate();
            preparedStatement = conexao.prepareStatement(sql);
            preparedStatement.setString(1, objeto.getDenunciante().getCpf());
            preparedStatement.setString(2, objeto.getAnalista().getCpf());
            preparedStatement.setInt(3, objeto.getEndereco().getMunicipio().getId());
            preparedStatement.setString(4, objeto.getEndereco().getCep());
            preparedStatement.setString(5, objeto.getEndereco().getLogradouro());
            preparedStatement.setString(6, objeto.getEndereco().getBairro());
            preparedStatement.setString(7, objeto.getEndereco().getPontoDeReferencia());
            preparedStatement.setString(8, objeto.getDescricao());
            preparedStatement.setInt(9, objeto.getCategoria().getId());
            preparedStatement.setInt(10, objeto.getCategoria().getSubCategoria().getIdSubcategoria());
            SimpleDateFormat formata = new SimpleDateFormat("dd/MM/yyyy");
            String datainclusao = formata.format(objeto.getData());
            preparedStatement.setString(11, datainclusao);
            preparedStatement.setString(12, objeto.getAutorCrime());
            preparedStatement.setString(13, objeto.getParecer());
            preparedStatement.setString(14, objeto.getStatus().toString());
            preparedStatement.setInt(15, idFotos);
            int idGerado = preparedStatement.executeUpdate();
            formata = new SimpleDateFormat("yyyy");
            String protocolo =  idGerado +"/"+ formata.format(objeto.getData());
            // Atualiza o protocolo na tabela gasto
            String sqlUpdate = "UPDATE denuncia SET protocolo = ? WHERE idDenuncia = ?";
            PreparedStatement updateStatement = conexao.prepareStatement(sqlUpdate);
            updateStatement.setString(1, protocolo);
            updateStatement.setInt(2, idGerado);
            updateStatement.executeUpdate();
            return protocolo;
        } catch (SQLException erro) {
            throw new Exception("SQL Erro: " + erro.getMessage());
        } catch(Exception erro){
            throw erro;
        }
    }

    @Override
    public Denuncia Buscar(String protocolo) throws Exception{
        String sql = "SELECT * FROM denuncia ";
        sql += "INNER JOIN municipio ON denuncia.idDenuncia = municipio.idMunicipio ";
        sql += "INNER JOIN categoria ON denuncia.idCategoria = categoria.idCategoria ";
        sql += "INNER JOIN fotos ON denuncia.idFotos = fotos.idFotos ";
        sql += "INNER JOIN usuario AS denunciante ON denuncia.cpfDenunciante = denunciante.cpf";
        sql += "INNER JOIN usuario AS analista ON denuncia.cpfAnalista = analista.cpf";
        try {
          Statement statement = conexao.createStatement();
          ResultSet rs = statement.executeQuery(sql);
          Denuncia denuncia = new Denuncia();
          if (rs.next()) {
            denuncia.setProtocolo(rs.getString("protocolo"));
            denuncia.setDescricao(rs.getString("descricao"));
            denuncia.setAutorCrime(rs.getString("autorCrime"));
            denuncia.setParecer(rs.getString("parecer"));
            denuncia.setStatus(Status.valueOf(rs.getString("status")));
            String data = rs.getString("data") ;
            Date date = new SimpleDateFormat("dd/MM/yyyy").parse(data);
            denuncia.setData(date);
            
            Fotos fotos = new Fotos();
            fotos.setId(rs.getInt("idFotos"));
            fotos.setUrl1(rs.getString("url1"));
            fotos.setUrl2(rs.getString("url2"));
            fotos.setUrl3(rs.getString("url3"));
            fotos.setUrl4(rs.getString("url4"));
            denuncia.setFotos(fotos);
            
            Usuario denunciante = new Usuario();
            denunciante.setCpf(rs.getString("cpf"));
            denunciante.setNome(rs.getString("nome"));
            denunciante.setEmail(rs.getString("email"));            
            denunciante.setTelefone(rs.getString("telefone"));
            denunciante.setTipo(Tipo.valueOf(rs.getString("tipo")));
            denuncia.setDenunciante(denunciante);
            
            Usuario analista = new Usuario();
            analista.setCpf(rs.getString("cpf"));
            analista.setNome(rs.getString("nome"));
            analista.setEmail(rs.getString("email"));            
            analista.setTelefone(rs.getString("telefone"));
            analista.setTipo(Tipo.valueOf(rs.getString("tipo")));
            denuncia.setAnalista(analista);

            Endereco endereco = new Endereco();
            endereco.setCoordenada(rs.getString("coordenada"));
            endereco.setCep(rs.getString("cep"));
            endereco.setLogradouro(rs.getString("logradouro"));
            endereco.setBairro(rs.getString("bairro"));
            endereco.setPontoDeReferencia(rs.getString("pontoDeReferencia"));
            
            Municipio municipio = new Municipio();
            municipio.setId(rs.getInt("idMunicipio"));
            municipio.setNome(rs.getString("nome"));
            municipio.setUf(rs.getString("uf"));
            municipio.setCodIBGE(rs.getInt("codIbge"));
            endereco.setMunicipio(municipio);
            denuncia.setEndereco(endereco);
            
            Categoria categoria = new Categoria();
            categoria.setId(rs.getInt("idCategoria"));
            categoria.getSubCategoria().setIdSubcategoria(rs.getInt("idSubcategoria"));
            categoria.setTipoAmbiental(rs.getString("tipo"));
            String sqlTipo = "SELECT * FROM "+categoria.getTipoAmbiental()+" WHERE idSubcategoria = "+categoria.getSubCategoria().getIdSubcategoria()+";";
            Statement statementTipo = conexao.createStatement();
            ResultSet rsTipo = statementTipo.executeQuery(sqlTipo);
            categoria.getSubCategoria().setSubTipoResumo(rsTipo.getString("resumo"));
            categoria.getSubCategoria().setSubTipo(rsTipo.getString("descricao"));
            denuncia.setCategoria(categoria);
            
          }
          return denuncia;
        } catch (SQLException erro) {
            throw new Exception("SQL Erro: " + erro.getMessage());
        } catch(Exception erro){
            throw erro;
        }

    }

    @Override
    public void Alterar(Denuncia denuncia) throws Exception{
        String sql = "UPDATE denuncia SET  idCategoria = ?,  idSubCategoria = ?  WHERE protocolo =" + denuncia.getProtocolo() + ";";
        PreparedStatement preparedStatement = conexao.prepareStatement(sql);
        preparedStatement.setInt(1, denuncia.getCategoria().getId());
        preparedStatement.setInt(2, denuncia.getCategoria().getSubCategoria().getIdSubcategoria());
        preparedStatement.executeUpdate();
    }

    @Override
    public ArrayList<Denuncia> Listar() throws Exception{
        ArrayList<Denuncia> listaDeDenuncias = new ArrayList<>();
        String sql = "SELECT * FROM denuncia ";
        sql += "INNER JOIN municipio ON denuncia.idDenuncia = municipio.idMunicipio ";
        sql += "INNER JOIN categoria ON denuncia.idCategoria = categoria.idCategoria ";
        sql += "INNER JOIN fotos ON denuncia.idFotos = fotos.idFotos ";
        sql += "INNER JOIN usuario AS denunciante ON denuncia.cpfDenunciante = denunciante.cpf";
        sql += "INNER JOIN usuario AS analista ON denuncia.cpfAnalista = analista.cpf";
        try {
          Statement statement = conexao.createStatement();
          ResultSet rs = statement.executeQuery(sql);
          while (rs.next()) {
            Denuncia denuncia = new Denuncia();
            denuncia.setProtocolo(rs.getString("protocolo"));
            denuncia.setDescricao(rs.getString("descricao"));
            denuncia.setAutorCrime(rs.getString("autorCrime"));
            denuncia.setParecer(rs.getString("parecer"));
            denuncia.setStatus(Status.valueOf(rs.getString("status")));
            String data = rs.getString("data") ;
            Date date = new SimpleDateFormat("dd/MM/yyyy").parse(data);
            denuncia.setData(date);
            
            Fotos fotos = new Fotos();
            fotos.setId(rs.getInt("idFotos"));
            fotos.setUrl1(rs.getString("url1"));
            fotos.setUrl2(rs.getString("url2"));
            fotos.setUrl3(rs.getString("url3"));
            fotos.setUrl4(rs.getString("url4"));
            denuncia.setFotos(fotos);
            
            Usuario denunciante = new Usuario();
            denunciante.setCpf(rs.getString("cpf"));
            denunciante.setNome(rs.getString("nome"));
            denunciante.setEmail(rs.getString("email"));            
            denunciante.setTelefone(rs.getString("telefone"));
            denunciante.setTipo(Tipo.valueOf(rs.getString("tipo")));
            denuncia.setDenunciante(denunciante);
            
            Usuario analista = new Usuario();
            analista.setCpf(rs.getString("cpf"));
            analista.setNome(rs.getString("nome"));
            analista.setEmail(rs.getString("email"));            
            analista.setTelefone(rs.getString("telefone"));
            analista.setTipo(Tipo.valueOf(rs.getString("tipo")));
            denuncia.setAnalista(analista);

            Endereco endereco = new Endereco();
            endereco.setCoordenada(rs.getString("coordenada"));
            endereco.setCep(rs.getString("cep"));
            endereco.setLogradouro(rs.getString("logradouro"));
            endereco.setBairro(rs.getString("bairro"));
            endereco.setPontoDeReferencia(rs.getString("pontoDeReferencia"));
            
            Municipio municipio = new Municipio();
            municipio.setId(rs.getInt("idMunicipio"));
            municipio.setNome(rs.getString("nome"));
            municipio.setUf(rs.getString("uf"));
            municipio.setCodIBGE(rs.getInt("codIbge"));
            endereco.setMunicipio(municipio);
            denuncia.setEndereco(endereco);
            
            Categoria categoria = new Categoria();
            categoria.setId(rs.getInt("idCategoria"));
            categoria.getSubCategoria().setIdSubcategoria(rs.getInt("idSubcategoria"));
            categoria.setTipoAmbiental(rs.getString("tipo"));
            String sqlTipo = "SELECT * FROM "+categoria.getTipoAmbiental()+" WHERE idSubcategoria = "+categoria.getSubCategoria().getIdSubcategoria()+";";
            Statement statementTipo = conexao.createStatement();
            ResultSet rsTipo = statementTipo.executeQuery(sqlTipo);
            categoria.getSubCategoria().setSubTipoResumo(rsTipo.getString("resumo"));
            categoria.getSubCategoria().setSubTipo(rsTipo.getString("descricao"));
            denuncia.setCategoria(categoria);
            
            listaDeDenuncias.add(denuncia);
          }
          return listaDeDenuncias;
        }catch (SQLException erro) {
            throw new Exception("SQL Erro: " + erro.getMessage());
        } catch(Exception erro){
            throw erro;
        } 
    }
    
}
