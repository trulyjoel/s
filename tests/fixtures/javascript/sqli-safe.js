/**
 * sqli-safe.js — safe parameterized query examples for Node.js/Express.
 * Each route handler is annotated with the expected security outcome.
 *
 * This file exists as a test fixture for the /appsec:check skill validation
 * suite. The skill should produce zero findings against this file.
 */

const express = require('express');
const router = express.Router();
const { pool } = require('../db/postgres');
const connection = require('../db/mysql');
const db = require('../db/sqlite');
const { User, Order } = require('../models');
const knex = require('../db/knex');
const { PrismaClient } = require('@prisma/client');

const prisma = new PrismaClient();

/**
 * GET /users/:id
 * Fetches a user by ID using node-postgres parameterized query ($1 placeholder).
 * EXPECT: NO FINDING
 */
router.get('/users/:id', async (req, res) => {
    const result = await pool.query(
        'SELECT * FROM users WHERE id = $1',
        [req.params.id]
    );
    res.json(result.rows[0]);
});

/**
 * POST /users/search
 * Searches users by email using mysql2 parameterized query (? placeholder).
 * EXPECT: NO FINDING
 */
router.post('/users/search', (req, res) => {
    connection.query(
        'SELECT * FROM users WHERE email = ?',
        [req.body.email],
        (err, results) => {
            res.json(results);
        }
    );
});

/**
 * DELETE /sessions
 * Deletes a session using sqlite3 parameterized query (? placeholder).
 * EXPECT: NO FINDING
 */
router.delete('/sessions', (req, res) => {
    const token = req.body.token;
    db.run("SELECT * FROM sessions WHERE token = ?", [token], (err) => {
        res.sendStatus(err ? 500 : 204);
    });
});

/**
 * GET /users/profile
 * Fetches a user via Sequelize model method — parameterized by construction.
 * EXPECT: NO FINDING
 */
router.get('/users/profile', async (req, res) => {
    const user = await User.findOne({
        where: { email: req.body.email }
    });
    res.json(user);
});

/**
 * GET /products
 * Fetches products using Knex query builder chain — parameterized by construction.
 * EXPECT: NO FINDING
 */
router.get('/products', async (req, res) => {
    const products = await knex('products')
        .where({ category: req.query.category })
        .select('*');
    res.json(products);
});

/**
 * GET /orders/:userId
 * Fetches orders using Prisma client — safe by construction.
 * EXPECT: NO FINDING
 */
router.get('/orders/:userId', async (req, res) => {
    const orders = await prisma.order.findMany({
        where: { userId: parseInt(req.params.userId, 10) }
    });
    res.json(orders);
});

/**
 * GET /users/:id/details
 * Fetches user details using node-postgres with multiple parameters.
 * EXPECT: NO FINDING
 */
router.get('/users/:id/details', async (req, res) => {
    const result = await pool.query(
        'SELECT * FROM users WHERE id = $1 AND status = $2',
        [req.params.id, 'active']
    );
    res.json(result.rows[0]);
});

module.exports = router;
