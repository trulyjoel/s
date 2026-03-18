/**
 * sqli-dangerous.js — intentionally vulnerable SQL examples for Node.js/Express.
 * Each route handler is annotated with the expected security finding.
 *
 * DO NOT USE IN PRODUCTION. This file exists solely as a test fixture
 * for the /appsec:check skill validation suite.
 */

const express = require('express');
const router = express.Router();
const { pool } = require('../db/postgres');
const connection = require('../db/mysql');
const db = require('../db/sqlite');
const { sequelize } = require('../db/sequelize');
const knex = require('../db/knex');

/**
 * GET /users/:id
 * Fetches a user by ID using a template literal passed to pool.query.
 * EXPECT: HIGH - SQL injection via req.params.id
 */
router.get('/users/:id', async (req, res) => {
    const result = await pool.query(
        `SELECT * FROM users WHERE id = ${req.params.id}`
    );
    res.json(result.rows);
});

/**
 * POST /users/search
 * Searches users by name using string concatenation with req.body.
 * EXPECT: HIGH - SQL injection via req.body.name
 */
router.post('/users/search', async (req, res) => {
    const sql = "SELECT * FROM users WHERE name = '" + req.body.name + "'";
    connection.query(sql, (err, results) => {
        res.json(results);
    });
});

/**
 * DELETE /sessions
 * Deletes a session token using string concatenation with sqlite3.
 * EXPECT: HIGH - SQL injection via token from req.body
 */
router.delete('/sessions', (req, res) => {
    const token = req.body.token;
    db.run("DELETE FROM sessions WHERE token = '" + token + "'", (err) => {
        res.sendStatus(err ? 500 : 204);
    });
});

/**
 * GET /orders
 * Fetches orders using Sequelize raw query with string concatenation.
 * EXPECT: HIGH - SQL injection via req.query.userId
 */
router.get('/orders', async (req, res) => {
    const userId = req.query.userId;
    const results = await sequelize.query(
        "SELECT * FROM orders WHERE user_id = " + userId
    );
    res.json(results[0]);
});

/**
 * GET /products
 * Searches products using knex.raw with string concatenation.
 * EXPECT: HIGH - SQL injection via req.query.category
 */
router.get('/products', async (req, res) => {
    const category = req.query.category;
    const results = await knex.raw(
        "SELECT * FROM products WHERE category = '" + category + "'"
    );
    res.json(results.rows);
});

/**
 * GET /users/profile
 * Fetches a user profile using a template literal with req.query input.
 * EXPECT: HIGH - SQL injection via req.query.email
 */
router.get('/users/profile', async (req, res) => {
    const result = await pool.query(
        `SELECT * FROM users WHERE email = '${req.query.email}'`
    );
    res.json(result.rows[0]);
});

module.exports = router;
