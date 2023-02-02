// get('/v2/featured', async (req, res, session) => {
//   const response = await req.propagate()
//   const data = response.json()

//   data.data[0].title = 'Mocked by Maestro Mock Server'

//   res.json(data);
// })

get('/v2/feature', async (req, res, session) => {
	const originalRes = await req.propagate();
	const data = originalRes.json();
  session.count = (session.count || 0) + 1;

  data.data[0].title = 'Mocked by Maestro Mock Server'

	res.status(200).json({
		...data,
		mockedResponse: true,
    session
	});
});