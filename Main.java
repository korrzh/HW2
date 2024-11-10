package PR2;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Вхідні дані від користувача
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введіть мінімальне значення діапазону:");
        int minRange = scanner.nextInt();
        System.out.println("Введіть максимальне значення діапазону:");
        int maxRange = scanner.nextInt();
        System.out.println("Введіть множник:");
        int multiplier = scanner.nextInt();

        // Генерація масиву випадкових чисел
        Random random = new Random();
        int arraySize = 40 + random.nextInt(21); // Розмір від 40 до 60
        int[] numbers = random.ints(arraySize, minRange, maxRange + 1).toArray();

        System.out.println("Згенерований масив:");
        for (int num : numbers) {
            System.out.print(num + " ");
        }
        System.out.println();
        long startTime = System.currentTimeMillis();
        // Створення пулу потоків
        ExecutorService executor = Executors.newCachedThreadPool();

        // Потокобезпечний список для зберігання результатів
        CopyOnWriteArrayList<Integer> resultList = new CopyOnWriteArrayList<>();

        // Запуск обробки масиву
        Future<Void> resultFuture = executor.submit(new DivideAndProcessTask(numbers, 0, numbers.length, multiplier, executor, resultList));

        Thread.sleep(20);

        if (!resultFuture.isDone()) {
            resultFuture.cancel(true);
        }
        if (resultFuture.isCancelled()) {
            System.err.println("future is cancelled");
        } else {
            // Очікуємо завершення основної задачі
            resultFuture.get();

            // Створення нового Future для копіювання даних
            Future<String> outputFuture = executor.submit(() -> {
                StringBuilder result = new StringBuilder();
                for (Integer num : resultList) {
                    result.append(num).append(" ");
                }
                return result.toString();
            });

            // Отримуємо результат виводу
            String resultOutput = outputFuture.get();
            System.out.printf("Результат з потоку: %s\n", resultOutput);

        }

        // Закриття пулу потоків
        executor.shutdown();

        // Виведення часу виконання
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Час роботи програми: " + duration + " мілісекунд");
    }

    // Callable для обробки масиву рекурсивно
    static class DivideAndProcessTask implements Callable<Void> {
        private final int[] array;
        private final int start;
        private final int end;
        private final int multiplier;
        private final ExecutorService executor;
        private final CopyOnWriteArrayList<Integer> resultList;

        public DivideAndProcessTask(int[] array, int start, int end, int multiplier, ExecutorService executor, CopyOnWriteArrayList<Integer> resultList) {
            this.array = array;
            this.start = start;
            this.end = end;
            this.multiplier = multiplier;
            this.executor = executor;
            this.resultList = resultList;
        }

        @Override
        public Void call() throws Exception {
            // Виведення поточного потоку
            System.out.println("Поточний потік: " + Thread.currentThread().getName() +
                    " обробляє елементи з індексами від " + start + " до " + (end - 1));

            if (end - start <= 10) { // Якщо частина масиву мала, обробляємо її безпосередньо
                for (int i = start; i < end; i++) {
                    resultList.add(array[i] * multiplier);
                }
            } else {
                // Розділяємо задачу на дві частини
                int mid = start + (end - start) / 2;
                System.out.println("Розділяємо задачу на дві частини: [" + start + ", " + mid + ") і [" + mid + ", " + end + ")");

                // Створення підзавдань для лівої та правої частини масиву
                Future<Void> leftFuture = executor.submit(new DivideAndProcessTask(array, start, mid, multiplier, executor, resultList));
                Future<Void> rightFuture = executor.submit(new DivideAndProcessTask(array, mid, end, multiplier, executor, resultList));

                // Чекаємо завершення обох частин

                leftFuture.get();
                rightFuture.get();
            }

            return null;
        }
    }
}