# Берём Java 17
FROM eclipse-temurin:17-jdk

# Рабочая папка внутри контейнера
WORKDIR /app

# Копируем pom.xml
COPY pom.xml .

# Копируем исходный код
COPY src ./src

# Собираем проект через Maven Wrapper, если он есть
# Если mvnw нет, ниже дам другой вариант
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Запускаем jar файл
CMD ["java", "-jar", "target/daramaven-0.0.1-SNAPSHOT.jar"]