import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TextAnalyzer {

    // Статические блокирующие очереди для символов 'a', 'b' и 'c'
    private static final int QUEUE_CAPACITY = 100;
    private static final BlockingQueue<String> queueA = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final BlockingQueue<String> queueB = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final BlockingQueue<String> queueC = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    // Флаг завершения генерации текстов
    private static volatile boolean isGenerating = true;

    public static void main(String[] args) {
        // Запуск потока генерации текстов
        Thread generatorThread = new Thread(new TextGenerator());
        generatorThread.start();

        // Запуск потоков анализа для 'a', 'b' и 'c'
        AnalyzerThread analyzerA = new AnalyzerThread(queueA, 'a');
        AnalyzerThread analyzerB = new AnalyzerThread(queueB, 'b');
        AnalyzerThread analyzerC = new AnalyzerThread(queueC, 'c');

        Thread threadA = new Thread(analyzerA);
        Thread threadB = new Thread(analyzerB);
        Thread threadC = new Thread(analyzerC);

        threadA.start();
        threadB.start();
        threadC.start();

        try {
            // Ожидание завершения генератора
            generatorThread.join();
            // После завершения генерации, уведомляем анализаторы
            isGenerating = false;
            // Разблокируем очереди, чтобы анализаторы могли завершиться
            queueA.put("EOF");
            queueB.put("EOF");
            queueC.put("EOF");

            // Ожидание завершения анализаторов
            threadA.join();
            threadB.join();
            threadC.join();

            // Вывод результатов
            System.out.println("Максимальное количество символов 'a' в строке: " + analyzerA.getMaxCount());
            System.out.println("Максимальное количество символов 'b' в строке: " + analyzerB.getMaxCount());
            System.out.println("Максимальное количество символов 'c' в строке: " + analyzerC.getMaxCount());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Основной поток был прерван.");
        }
    }

    // Класс генератора текстов
    static class TextGenerator implements Runnable {
        private static final String LETTERS = "abc";
        private static final int TEXT_LENGTH = 100_000;
        private static final int TEXT_COUNT = 10_000;

        @Override
        public void run() {
            for (int i = 0; i < TEXT_COUNT; i++) {
                String text = generateText(LETTERS, TEXT_LENGTH);
                try {
                    // Добавляем текст в каждую из очередей
                    queueA.put(text);
                    queueB.put(text);
                    queueC.put(text);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Генератор был прерван.");
                    break;
                }
            }
        }

        // Метод генерации текста
        public static String generateText(String letters, int length) {
            Random random = new Random();
            StringBuilder text = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                text.append(letters.charAt(random.nextInt(letters.length())));
            }
            return text.toString();
        }
    }

    // Класс анализатора для каждого символа
    static class AnalyzerThread implements Runnable {
        private final BlockingQueue<String> queue;
        private final char targetChar;
        private int maxCount = 0;

        public AnalyzerThread(BlockingQueue<String> queue, char targetChar) {
            this.queue = queue;
            this.targetChar = targetChar;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String text = queue.take();
                    // Проверка на сигнал окончания
                    if (text.equals("EOF")) {
                        break;
                    }
                    int count = countChar(text, targetChar);
                    if (count > maxCount) {
                        maxCount = count;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Анализатор для '" + targetChar + "' был прерван.");
            }
        }

        // Метод подсчета количества символов
        private int countChar(String text, char target) {
            int count = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == target) {
                    count++;
                }
            }
            return count;
        }

        public int getMaxCount() {
            return maxCount;
        }
    }
}