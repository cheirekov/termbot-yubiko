FROM ghcr.io/cirruslabs/android-sdk:34

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.27%2B6/OpenJDK11U-jdk_x64_linux_hotspot_11.0.27_6.tar.gz" \
      -o /tmp/jdk11.tar.gz \
    && mkdir -p /opt \
    && tar -xzf /tmp/jdk11.tar.gz -C /opt \
    && ln -s /opt/jdk-11.0.27+6 /opt/jdk11 \
    && rm -f /tmp/jdk11.tar.gz

# Preinstall the legacy NDK expected by this project so Gradle doesn't try to
# install it at runtime (container runs as non-root user).
RUN JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin:${PATH}" \
    yes | sdkmanager --sdk_root=/opt/android-sdk-linux \
        "platforms;android-29" \
        "build-tools;29.0.3" \
        "cmake;3.10.2.4988404" \
        "ndk;21.3.6528147"

ENV JAVA_HOME=/opt/jdk11
ENV PATH="/opt/jdk11/bin:${PATH}"
