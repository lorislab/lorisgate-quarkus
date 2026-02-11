# Lorisgate - Quarkus Dev Service

> **Repository Description**: Quarkus extension providing Dev Services for Lorisgate. Automatically starts and configures a Lorisgate instance using Testcontainers for local development and integration testing, ensuring a seamless "zero-config" experience.

---

[![Maven Central](https://img.shields.io/maven-central/v/org.lorislab.lorisgate/lorisgate-quarkus?style=for-the-badge)](https://search.maven.org/search?q=g:org.lorislab.lorisgate%20AND%20a:lorisgate-quarkus)
[![GitHub Release](https://img.shields.io/github/v/release/lorislab/lorislab-quarkus-lorisgate?style=for-the-badge)](https://github.com/lorislab/lorislab-quarkus-lorisgate/releases)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)

## Overview

**Lorisgate Dev Service** is a Quarkus extension that manages a Lorisgate instance during development and testing. It leverages [Testcontainers](https://www.testcontainers.org/) to spin up a Docker container automatically, so you don't have to manually install or manage Lorisgate processes.

When Quarkus starts in **Dev Mode** (`quarkus dev`) or **Test Mode** (`maven test`), the extension checks if a Lorisgate connection is already configured. If not, it pulls the image, starts the container, and injects the connection properties directly into your application.

## Features

* 🚀 **Zero-Config**: Get up and running without touching `application.properties`.
* 🔄 **Lifecycle Management**: Automatically starts on boot and stops when the Quarkus process ends.
* 🤝 **Shared Instances**: Keeps the container running across live-reloads to save time.
* 🛠️ **Customizable**: Easily override the image version or port if needed.

## Installation

Add the following dependency to your Quarkus project's `pom.xml`:

```xml
<dependency>
   <groupId>org.lorislab.lorisgate</groupId>
   <artifactId>lorisgate-quarkus</artifactId>
</dependency>
```

## How it Works

1. Detection: The extension looks for lorisgate.url.
2. Provisioning: If missing, it starts a Lorisgate container via Docker.
3. Injection: It dynamically provides the following properties to the Quarkus runtime:
    - lorisgate.host
    - lorisgate.port

## Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes
4. Push to your fork
5. Open a Pull Request

## License

Licensed under the Apache License, Version 2.0.