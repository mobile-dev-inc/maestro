get('/v2/featured', async (req, res, session) => {
  const response = await req.propagate()
  const data = response.json()

  session.hits = (session.hits || 0) + 1

  data.data[0].title = 'Mocked by Maestro'

  res.json({
    hits: session.hits,
    ...data
  })
})
