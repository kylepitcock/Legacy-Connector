# Headless Linux environment for the graphical dev tests (Xvfb + Mesa software rendering), see tools/dev/docker-headless.sh.
FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb zsh procps rsync unzip libgl1 libgl1-mesa-dri libglx-mesa0 libglu1-mesa libopenal1 \
    libxrender1 libxtst6 libxi6 libxrandr2 libxcursor1 libxxf86vm1 libxinerama1 libxkbcommon0 \
  && rm -rf /var/lib/apt/lists/*
ENV LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450
