#!/bin/bash

bash ./build.sh
java -cp ./../../../../../../lib/junit-4.11.jar:./../../../../../../lib/hamcrest-core-1.3.jar:out org.junit.runner.JUnitCore info.kgeorgiy.ja.shcherbakov.bank.BankTest
