# Omegizent

AI-powered assistant that helps you explore, understand, and develop software projects.

## Early Development

> [!WARNING]
> Omegizent is in early development and should be considered a prototype or proof of concept.
> It is not yet ready for general use.
> Expect incomplete features, experimental ideas, and frequent iteration.

The current early development goal for Omegizent is to develop a minimal functional implementation
that is able to ingest basic information about a GitHub project into a vectorized knowledge base,
allow a user to make a query about the project,
use retrieval-augmented generation to send the user’s query
along with relevant portions of the project to the ChatGPT API,
and display the ChatGPT API response to the user.

## Development Process

Although Omegizent is currently a one-person project,
it is being developed using a collaborative, team-oriented workflow.
Each feature or idea is tracked as a GitHub issue,
and changes are implemented through topic branches and pull requests.
This workflow has been intentionally chosen to:

- Generate realistic issue and pull request data that Omegizent will eventually analyze and learn from.
- Simulate real-world team collaboration, enabling more robust development and testing of Omegizent's capabilities.
- Explore and validate how Omegizent can be used in complex projects with structured workflows.

## Prerequisites

Omegizent is primarily written in Java but also uses Python for some tasks.
It also uses the OpenAI API and Qdrant.

### Java

You need a Java JDK and [Apache Maven](https://maven.apache.org/) to build and run Omegizent.
We use the [Eclipse Temurin](https://adoptium.net/temurin/) Java JDK, but other JDKs may also work.

Omegizent is tested with the following versions, but other versions may also work:

- Eclipse Temurin:
  - `25.0.4+7-LTS` (Windows x64)
- Apache Maven:
  - `3.9.16`

### Python

You need [Python](https://www.python.org/) and [Poetry](https://python-poetry.org/) to run Omegizent.

Omegizent is tested with the following versions, but other versions may also work:

- Python:
  - `3.14.7`
- Poetry:
  - `2.5.1`

Ensure that your Python dependencies are installed and updated before running Omegizent.
Run the following in the `python-tools` directory:

```bash
poetry sync
```

### OpenAI API

You need an [OpenAI API](https://openai.com/api/) key to run Omegizent.

If you use an API key with *Restricted* permissions, you must grant *Write* permission to the following resources:
- Model Capabilities
- Responses API

### Qdrant

You need a [Qdrant](https://qdrant.tech/) database to run Omegizent.

We provide instructions for running Qdrant in a Docker container, but other options are available,
such as [Qdrant Cloud](https://qdrant.tech/documentation/cloud-quickstart/).

Ensure that [Docker Engine](https://docs.docker.com/engine/install/) is installed before proceeding.

Qdrant images are available from Docker Hub: https://hub.docker.com/r/qdrant/qdrant

To download or update the Qdrant Docker image: `docker pull qdrant/qdrant:v1.19.0`

To create a Docker container for Qdrant:

```bash
docker create --name qdrant \
    -p 6333:6333 -p 6334:6334 \
    -v qdrant-storage:/qdrant/storage \
    qdrant/qdrant:v1.19.0
```

This command does the following:

- Assigns the name `qdrant` to the container (`--name`).
- Opens network port 6333 for the Qdrant REST API (`-p`).
- Opens network port 6334 for the Qdrant gRPC API (`-p`).
- Attaches the `qdrant-storage` volume to `/qdrant/storage` for storing Qdrant's data (`-v`).

To start the Qdrant Docker container: `docker start qdrant`

Verify that Qdrant is running by checking its web interface
(replace `qdrant-host` with the hostname or IP of your Qdrant database):
http://qdrant-host:6333/dashboard

To stop the Qdrant Docker container: `docker stop qdrant`

To remove the Qdrant Docker container: `docker rm qdrant`

Removing the Qdrant Docker container does not remove the `qdrant-storage` volume.

To remove the `qdrant-storage` volume: `docker volume rm qdrant-storage`

## Configuration

Omegizent requires the following environment variables to be set:

- `OMEGIZENT_OPENAI_API_KEY`: Your OpenAI API key.
- `OMEGIZENT_QDRANT_HOST`: The host name or IP address of your Qdrant database.
- `OMEGIZENT_QDRANT_GRPC_PORT`: The gRPC port of your Qdrant database, most likely `6334`.

We use [dotenv-java](https://github.com/cdimascio/dotenv-java)
to allow environment variables to be specified in a file.
To do so, create a file named `.env` in your project root directory with the following:

```env
OMEGIZENT_OPENAI_API_KEY=openai-api-key
OMEGIZENT_QDRANT_HOST=qdrant-host
OMEGIZENT_QDRANT_GRPC_PORT=qdrant-grpc-port
```

Replace the values with settings appropriate for your environment.

> [!CAUTION]
> Do not commit `.env` to Git.
> Doing so will leak sensitive information.

## Building and Running

To build Omegizent and run the unit tests: `mvn package`

To run the integration tests: `mvn verify`

To run the *Graphical Query Interface*: `mvn javafx:run`

To run the *Command-Line Query Interface*: `mvn exec:exec`

To run the *Embedding* proof of concept: `mvn exec:exec -P embed`

To run the *Split Text* proof of concept: `mvn exec:exec -P split-text -D exec.filename=[filename]`

Replace `[filename]` with the name of the text split to split.

To run the *Split Markdown* proof of concept: `mvn exec:exec -P split-markdown`

To run the *Qdrant* proof of concept: `mvn exec:exec -P qdrant`

## Code Coverage

Generate a code coverage report:

```bash
mvn clean jacoco:prepare-agent verify jacoco:report
```

Find the report in: `target/site/jacoco/`

## License

```text
Copyright 2025-2026 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
