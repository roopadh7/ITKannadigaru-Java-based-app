package com.itkannadigaru.repository;

import com.itkannadigaru.model.Post;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PostRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Post save(Post post) {
        String sql = "INSERT INTO posts (title, content, author, created_at, category) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            ps.setString(3, post.getAuthor());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, post.getCategory());
            return ps;
        }, keyHolder);

        post.setId(keyHolder.getKey().longValue());
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }

    public List<Post> findAll() {
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new PostRowMapper());
    }

    public List<Post> findByCategory(String category) {
        String sql = "SELECT * FROM posts WHERE category = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new PostRowMapper(), category);
    }

    public Post findById(Long id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        List<Post> posts = jdbcTemplate.query(sql, new PostRowMapper(), id);
        return posts.isEmpty() ? null : posts.get(0);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private static class PostRowMapper implements RowMapper<Post> {
        @Override
        public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
            Post post = new Post();
            post.setId(rs.getLong("id"));
            post.setTitle(rs.getString("title"));
            post.setContent(rs.getString("content"));
            post.setAuthor(rs.getString("author"));

            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                post.setCreatedAt(timestamp.toLocalDateTime());
            }

            post.setCategory(rs.getString("category"));
            return post;
        }
    }
}
