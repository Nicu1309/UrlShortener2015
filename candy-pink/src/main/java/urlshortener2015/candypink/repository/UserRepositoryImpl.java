package urlshortener2015.common.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import urlshortener2015.common.domain.Click;


@Repository
public class ClickRepositoryImpl implements ClickRepository {

	private static final Logger log = LoggerFactory
			.getLogger(UserRepositoryImpl.class);

	private static final RowMapper<User> rowMapper = new RowMapper<User>() {
		@Override
		public User mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Click(rs.getString("name"), rs.getString("type"));
		}
	};

	@Autowired
	protected JdbcTemplate jdbc;

	public UserRepositoryImpl() {
	}

	public UserRepositoryImpl(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public User findByName(String name) {
		try {
			return jdbc.query("SELECT * FROM user WHERE name=?",
					new Object[] { name }, rowMapper);
		} catch (Exception e) {
			log.debug("When select for name " + hash, e);
			return null;
		}
	}

	@Override
	public User save(final User user) {
		try {
			KeyHolder holder = new GeneratedKeyHolder();
			jdbc.update(new PreparedStatementCreator() {

				@Override
				public PreparedStatement createPreparedStatement(Connection conn)
						throws SQLException {
					PreparedStatement ps = conn
							.prepareStatement(
									"INSERT INTO USER VALUES (?, ?)",
									Statement.RETURN_GENERATED_KEYS);
					ps.setString(1, user.getName());
					ps.setString(2, user.getHash());
				}
			}, holder);
			new DirectFieldAccessor(cl).setPropertyValue("id", holder.getKey()
					.longValue());
		} catch (DuplicateKeyException e) {
			log.debug("When insert for user with user " + user.getName(), e);
			return cl;
		} catch (Exception e) {
			log.debug("When insert a user", e);
			return null;
		}
		return cl;
	}

	@Override
	public void update(Click cl) {
		log.info("ID2: "+cl.getId()+"navegador: "+cl.getBrowser()+" SO: "+cl.getPlatform()+" Date:"+cl.getCreated());
		try {
			jdbc.update(
					"update click set hash=?, created=?, referrer=?, browser=?, platform=?, ip=?, country=? where id=?",
					cl.getHash(), cl.getCreated(), cl.getReferrer(),
					cl.getBrowser(), cl.getPlatform(), cl.getIp(),
					cl.getCountry(), cl.getId());
			
		} catch (Exception e) {
			log.info("When update for id " + cl.getId(), e);
		}
	}

	@Override
	public void delete(Long id) {
		try {
			jdbc.update("delete from click where id=?", id);
		} catch (Exception e) {
			log.debug("When delete for id " + id, e);
		}
	}

	@Override
	public void deleteAll() {
		try {
			jdbc.update("delete from User");
		} catch (Exception e) {
			log.debug("When delete all", e);
		}
	}

	@Override
	public Long count() {
		try {
			return jdbc
					.queryForObject("select count(*) from click", Long.class);
		} catch (Exception e) {
			log.debug("When counting", e);
		}
		return -1L;
	}

	@Override
	public List<Click> list(Long limit, Long offset) {
		try {
			return jdbc.query("SELECT * FROM click LIMIT ? OFFSET ?",
					new Object[] { limit, offset }, rowMapper);
		} catch (Exception e) {
			log.debug("When select for limit " + limit + " and offset "
					+ offset, e);
			return null;
		}
	}

	@Override
	public Long clicksByHash(String hash) {
		try {
			return jdbc
					.queryForObject("select count(*) from click where hash = ?", new Object[]{hash}, Long.class);
		} catch (Exception e) {
			log.debug("When counting hash "+hash, e);
		}
		return -1L;
	}

}
