<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PharmaLink Dashboard</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            display: flex;
            flex-direction: column;
            min-height: 100vh;
        }
        header {
            background-color: #28a745; /* Ein schönes Grün für Pharma */
            color: white;
            padding: 1rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .logo {
            font-size: 1.8rem;
            font-weight: bold;
        }
        nav ul {
            list-style: none;
            margin: 0;
            padding: 0;
            display: flex;
        }
        nav ul li {
            margin-left: 2rem;
        }
        nav ul li a {
            color: white;
            text-decoration: none;
            font-weight: bold;
            transition: color 0.3s ease;
        }
        nav ul li a:hover {
            color: #d4edda; /* Helleres Grün beim Hover */
        }
        main {
            flex: 1; /* Nimmt den restlichen Platz ein */
            padding: 2rem;
            text-align: center;
        }
        footer {
            background-color: #343a40; /* Dunkles Grau für den Footer */
            color: white;
            text-align: center;
            padding: 1rem 0;
            box-shadow: 0 -2px 4px rgba(0,0,0,0.1);
        }
        footer a {
            color: #6c757d; /* Helleres Grau für den Link */
            text-decoration: none;
            transition: color 0.3s ease;
        }
        footer a:hover {
            color: #f8f9fa; /* Fast weiß beim Hover */
        }
    </style>
</head>
<body>
<header>
    <div class="logo">PharmaLink</div>
    <nav>
        <ul>
            <li><a href="/actors">Actor</a></li>
            <li><a href="/medications">Medikament</a></li>
            <li><a href="/units">Unit</a></li>
        </ul>
    </nav>
</header>

<main>
    <h1>Willkommen bei deinem PharmaLink Dashboard!</h1>
    <p>Hier siehst du eine Übersicht deiner wichtigen Informationen.</p>
</main>

<footer>
    <p>
        Weitere Informationen findest du in der
        <a href="/api/swagger-ui/index.html" target="_blank">API Dokumentation</a>.
    </p>
</footer>
</body>
</html>