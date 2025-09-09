<html>
<head>
    <title>Logout Confirmation</title>
</head>
<body>
    <h1>Are you sure you want to log out?</h1>
    <form action="${url.logout}" method="post">
        <button type="submit">Yes</button>
    </form>
    <form action="${url.cancel}" method="get">
        <button type="button" onclick="window.location.href='${url.cancel}'">No</button>
    </form>
</body>
</html>
