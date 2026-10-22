# Headless Linux environment for the graphical dev tests (Xvfb + Mesa software rendering), see tools/dev/docker-headless.sh.
FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb zsh procps rsync unzip curl ca-certificates libgl1 libgl1-mesa-dri libglx-mesa0 libglu1-mesa libopenal1 \
    libxrender1 libxtst6 libxi6 libxrandr2 libxcursor1 libxxf86vm1 libxinerama1 libxkbcommon0 \
  && rm -rf /var/lib/apt/lists/*
ENV LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450
# The game's own launcher metadata only lists x64 Linux natives: on an arm64 host fetch LWJGL's arm64 ones.
RUN mkdir -p /opt/lwjgl && if [ "$(uname -m)" = "aarch64" ]; then mkdir /tmp/lw && cd /tmp/lw && \
    for m in lwjgl lwjgl-glfw lwjgl-opengl lwjgl-openal lwjgl-stb lwjgl-tinyfd lwjgl-jemalloc; do \
      curl -fsSL -o $m.jar https://repo1.maven.org/maven2/org/lwjgl/$m/3.3.3/$m-3.3.3-natives-linux-arm64.jar && unzip -qo $m.jar -d x; \
    done && find x -name "*.so" -exec cp {} /opt/lwjgl/ \; && rm -rf /tmp/lw; fi
ENV JAVA_TOOL_OPTIONS="-Dorg.lwjgl.librarypath=/opt/lwjgl"
