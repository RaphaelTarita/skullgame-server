# SkullGame Server

Skull and Roses is a simple card game. You can read more about
it [here](https://en.wikipedia.org/wiki/Skull_(card_game)). This repository contains a game server implementation of the
game logic and all the surrounding infrastructure, for example authentication, game rooms, and hosting multiple running
games at once.

This is an HTTP(S) server (there might be a WebSockets part later, to avoid polling logic in the clients) that uses JWK
for authentication. It is implemented with [Ktor](https://ktor.io/) and also uses [Logback](https://logback.qos.ch/)
and [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime). Additionally, there's a dependency
on [SkullGame Common](https://github.com/RaphaelTarita/skullgame-common), which is not published on any public package
registry. Therefore, in order to build this server, you have to first build skullgame-common and install it into your
local maven repository (see 'Building' for more information).

### Endpoints

- `/login`: authenticate via JWK and get a token for all the other endpoints
- `/player/hello`: validate auth
- `/player/games`: query which games you have joined
- `/newgame`: open up a new game room
- `/join/{gameid}`: join a game room
- `/gameinfo/{gameid}`: query information about all users in the game room
- `/startgame/{gameid}`: start a game (if you're the initiator)
- `/move/{gameid}`: submit a move to a running game
- `/masterstate/{gameid}`: query the master state of a game (if you're an admin)
- `/state/{gameid}`: query your individual player state
- `/.well-known/jwks.json`: JWK Set for key validation

### Building

SkullGame Server uses [Gradle](https://gradle.org/) as a build system. To build and run this server, it is generally
enough to invoke `gradlew run` (or `gradlew.bat run` on windows).

All dependencies will be pulled in automatically, but
there is one exception: `com.rtarita.skull:skullgame-common` contains common code that can be shared between server and
client. You can find the [repository on GitHub](https://github.com/RaphaelTarita/skullgame-common) as well, but there
are no published packages of it, which means you have to build it yourself.

Another important thing to note is that the server expects a `server-config` directory in the same folder as it is run
in. This directory has to contain the following files:

- `certs/jwks.json`: JWK Set, see 'Configuration' for details on how to generate
- `config.json`: general-purpose configurations for the server, see 'Configuration' for detailed info
- `id_rsa`: Matching private key to `jwks.json`.

Ultimately, this means you have to progress through the following steps to build and run this server:

1. Clone [skullgame-common](https://github.com/RaphaelTarita/skullgame-common)
2. Follow the instructions from its README to build it and install it into the local maven repository
3. Create a new directory called `server-config` directly below the root directory of this project
4. Fill that directory with the files mentioned above (see 'Configuration' for more info)
5. Invoke `gradlew run` / `gradlew.bat run` in a terminal

### Configuration

#### `jwks.json` and `id_rsa`:

JWK needs a private/public keypair to create and sign the tokens that are used for authentication in this server.
However, the public key needs to be in a special JSON format. In order to obtain a (correctly formatted) keypair, you
can use [mkjwk](https://mkjwk.org/).

> [!CAUTION]
> mkjwk will generate a `jwks.json` file that **also contains the private key**! If you leave the
> private key in the file, it will be published by your server, completely exposing every single endpoint and allowing
> arbitrary user spoofing. **READ THE STEPS BELOW CAREFULLY TO AVOID THIS**

1. Go to https://mkjwk.org/
2. Select "RSA" (should be preselected)
3. Key Size: The default is 2048, which is fine. If you know what you are doing, you can change this
4. Key Use: Signature
5. Algorithm: RSA256 (RSASSA-PKCS1-v1_5 using SHA-256)
6. Key ID: SHA-256
7. Show X.509: Yes
8. Click 'Generate'
9. Copy 'Private Key (X.509 PEM Format)' and paste it into the `id_rsa` file, then remove the 'BEGIN PRIVATE KEY' and '
   END PRIVATE KEY' lines. This is your private key, keep it secret at all times.
10. The correct format for the `certs/jwks.json` file is only found in the 'Public and Private Keypair Set' result, but
    this contains the private key as well. Therefore, **DO NOT COPY** the 'Public and Private Keypair Set' result.
    Instead, put the following JSON structure into the file:
    ```json
    {
        "keys": [
            
        ]
    }
    ```
    Then, copy the 'Public Key' result from mkjwk and paste it inside the square brackets of the `keys` property in your
    JSON. This will now only contain the public key. Here is an example of how your `jwks.json` might look like:
    ```json
    {
        "keys": [
            {
                "kty": "RSA",
                "e": "AQAB",
                "use": "sig",
                "kid": "ryjwaNbswkJDS4HTUD07_DVTglMUL7H9sblugWJTd5s",
                "alg": "RS256",
                "n": "wKJ3kS-iB54CD8hB8QkOfX9C3oLJC8NkEKmrjH3gEy7gw... (and so on)"
            }
        ]
    }
    ```
    If your file contains any other properties than the ones in this example, **REMOVE THEM**.

This is all you have to do for your `server-config/id_rsa` and `server-config/certs/jwks.json` files.

#### `config.json`:

The configuration file for the SkullGame Server has the following structure:

```json
{
  "auth": {
    "issuer": "<some url>",
    "audience": "<some url>",
    "realm": "<short description>",
    "kid": "a JWK key ID"
  },
  "users": [
    {
      "id": "<user id>",
      "displayName": "<user display name>",
      "passHash": "<hash of users password>",
      "isAdmin": false
    },
    ...
  ],
  "environment": {
    "host": "<some host>",
    "subdomain": "<some subdomain>",
    "port": 8443,
    "maxManagerThreads": 4
  },
  "features": {
    "reverseProxy": false,
    "autoHeadResponse": true,
    "siteServing": false
  }
}
```

Explanation:

- `auth.issuer`: the JWK Issuer URL, in this case it's just the URL on which the server is running
- `auth.audience`: you can re-use the issuer URL for this
- `auth.realm`: something like "access to the SkullGame Server"
- `auth.kid`: copy and paste the `kid` property of your `certs/jwks.json` file into this property
- `users[]`: in this array you can define all the users that have access to the game server and can use their account to
  play games
- `users[].id`: unique user ID that is used for logging in
- `users[].displayName`: the users' display name
- `users[].passHash`: the SHA-256 hash (hexadecimal, lowercase) of the users' password
- `users[].admin` (default: `false`): whether the user has admin rights or not
- `environment.host`: The host domain on which the server is running
- `environment.port` (default: `8443`): The port on which the server should listen
- `environment.maxManagerThreads` (default: `4`): The maximum number of threads that will be allocated specifically to
  games management. The actual number of threads will be at most this number, but capped by the number of logical
  processors of the machine
- `features.reverseProxy` (default: `false`): set this to `true` if you're running your server behind a reverse proxy
  (it will cause IPs to be resolved correctly for logging and similar stuff). If you don't run the server behind a
  reverse proxy, **set this to `false`**!
- `features.autoHeadResponse` (default: `true`): Whether the server should automatically answer `HEAD` requests for its
  endpoints
- `features.siteServing` (default: `false`): Serve a static website alongside your server. The static site will be
  available without authentication. If set to `true`, the server will look for files in a directory called `site` (at
  the same location as the `server-config` directory) and statically serve all contents of that directory from the `/`
  route.

Once you have configured all these settings and put them in `server-config/config.json`, you are ready to run the
server!