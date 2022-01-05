package com.github.piorrro33.paktools;

import java.util.Scanner;

public class Utils {
    private static final Scanner SCANNER = new Scanner(System.in);

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Prints the {@code message} parameter and awaits a "yes" or "no" answer on standard input.
     *
     * @param message The {@code String} to be printed to the user.
     * @return {@code true} if the user replied "yes", {@code false} if the user replied "no".
     */
    public static boolean promptYesOrNo(String message) {
        System.out.print(message);

        boolean isValidAnswer = false, answer = false;
        while (!isValidAnswer) {
            final String userAnswer = SCANNER.nextLine();

            if (userAnswer.equalsIgnoreCase("yes") || userAnswer.equalsIgnoreCase("y")) {
                isValidAnswer = true;
                answer = true;
            } else if (userAnswer.equalsIgnoreCase("no") || userAnswer.equalsIgnoreCase("n")) {
                isValidAnswer = true;
            } else {
                System.out.print("Please enter \"yes\" or \"no\": ");
            }
        }

        return answer;
    }
}
