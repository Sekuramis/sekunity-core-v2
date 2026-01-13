package eu.sekunity.paper.infra.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.zaxxer.hikari.HikariDataSource;

import eu.sekunity.api.database.Database;
import eu.sekunity.api.database.ResultSetMapper;

/**
 * © Copyright 11.01.2026 - 17:44 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public class HikariDatabase implements Database
{
	private final HikariDataSource dataSource;
	private final Executor executor;

	public HikariDatabase(final HikariDataSource dataSource, final Executor executor)
	{
		this.dataSource = dataSource;
		this.executor = executor;
	}

	@Override
	public CompletableFuture<Integer> update(String sql, Object... params)
	{
		return CompletableFuture.supplyAsync(() -> {
			try (Connection con = dataSource.getConnection();
					PreparedStatement ps = con.prepareStatement(sql))
			{
				bind(ps, params);
				return ps.executeUpdate();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}, executor);
	}

	@Override
	public <T> CompletableFuture<T> queryOne(String sql, ResultSetMapper<T> mapper, Object... params)
	{
		return CompletableFuture.supplyAsync(() -> {
			try (Connection con = dataSource.getConnection();
					PreparedStatement ps = con.prepareStatement(sql))
			{
				bind(ps, params);
				try (ResultSet rs = ps.executeQuery())
				{
					if (!rs.next())
						throw new IllegalStateException("Expected one row but got none");

					return mapper.map(rs);
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}, executor);
	}

	@Override
	public <T> CompletableFuture<Optional<T>> queryOneOptional(String sql, ResultSetMapper<T> mapper, Object... params)
	{
		return CompletableFuture.supplyAsync(() -> {
			try (Connection con = dataSource.getConnection();
					PreparedStatement ps = con.prepareStatement(sql))
			{
				bind(ps, params);
				try (ResultSet rs = ps.executeQuery())
				{
					if (!rs.next())
						return Optional.empty();

					return Optional.ofNullable(mapper.map(rs));
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}, executor);
	}

	@Override
	public <T> CompletableFuture<List<T>> query(String sql, ResultSetMapper<T> mapper, Object... params)
	{
		return CompletableFuture.supplyAsync(() -> {
			try (Connection con = dataSource.getConnection();
					PreparedStatement ps = con.prepareStatement(sql))
			{
				bind(ps, params);
				try (ResultSet rs = ps.executeQuery())
				{
					List<T> out = new ArrayList<>();
					while (rs.next())
						out.add(mapper.map(rs));
					return out;
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}, executor);
	}

	private static void bind(PreparedStatement ps, Object... params) throws SQLException
	{
		if (params == null) return;
		for (int i = 0; i < params.length; i++)
			ps.setObject(i + 1, params[i]);
	}

	@Override
	public void close()
	{
		dataSource.close();
	}

	public static HikariDatabase create(String jdbcUrl, String user, String pass, int poolSize, Executor async)
	{
		com.zaxxer.hikari.HikariConfig hc = new com.zaxxer.hikari.HikariConfig();

		hc.setJdbcUrl(jdbcUrl);
		hc.setUsername(user);
		hc.setPassword(pass);
		hc.setMaximumPoolSize(poolSize);
		hc.setPoolName("Sekunity-Paper-Core");

		hc.setDriverClassName("org.mariadb.jdbc.Driver");
		hc.addDataSourceProperty("useUnicode", "true");
		hc.addDataSourceProperty("characterEncoding", "utf8mb4");

		return new HikariDatabase(new com.zaxxer.hikari.HikariDataSource(hc), async);
	}

}
