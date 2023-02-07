get('/posts', (req, res, session) => {
    session.count = (session.count || 0) + 1;

    res.json({
        posts: [1, 2, 3].map(i => ({ id: i, title: `Post $\{i\}` }))
    });
});