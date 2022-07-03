from sklearn.tree import DecisionTreeClassifier
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.naive_bayes import GaussianNB
from sklearn.neural_network import MLPClassifier
from sklearn.linear_model import SGDClassifier
from sklearn.model_selection import cross_validate
import pandas as pd
import statistics
import warnings
import sys
import os


# clear warnings
def warn(*args, **kwargs):
    pass


warnings.warn = warn


# create header for output table
def create_output_columns(num_folds):
    columns = ["Model"]
    for i in range(num_folds):
        columns.append("Fold " + str(i+1))

    columns.append("AVG")
    return columns


# add model split to output table
def create_output_row(cross_val, model):
    # get accuracies
    splits = list(map(lambda x: "%.2f%%" % x, cross_val["test_score"] * 100))
    cv_avg = "{:.2f}%".format(cross_val["test_score"].mean() * 100)

    # add model row to output string
    row = [model]

    # add accuracy of each split to each fold column
    for split in splits:
        row.append(split)
    row.append(cv_avg)

    return row


# file
path = sys.argv[1]

# number of folds
folds = int(sys.argv[2])

# get models
# allow customization of models later
models = ["DT", "SGD", "NB", "SVM", "KNN", "LR", "LDA", "MLP", "RF"]
all_folds = []
output = []

if os.path.exists(path):
    # create dataframe from file
    dataframe = pd.read_csv(path)

    # get data and labels
    labels = dataframe["class"].values.reshape(-1, 1)
    data = dataframe.drop(["class"], axis=1)

    # run decision tree
    cv = cross_validate(DecisionTreeClassifier(), data, labels, cv=folds)
    output.append(create_output_row(cv, "DT"))
    all_folds.append(cv["test_score"])

    # run SGD
    cv = cross_validate(SGDClassifier(), data, labels, cv=folds)
    output.append(create_output_row(cv, "SGD"))
    all_folds.append(cv["test_score"])

    # run naive bayes
    cv = cross_validate(GaussianNB(), data, labels, cv=folds)
    output.append(create_output_row(cv, "NB"))
    all_folds.append(cv["test_score"])

    # run support vector machine
    cv = cross_validate(SVC(), data, labels, cv=folds)
    output.append(create_output_row(cv, "SVM"))
    all_folds.append(cv["test_score"])

    # run k nearest neighbors
    cv = cross_validate(KNeighborsClassifier(), data, labels, cv=folds)
    output.append(create_output_row(cv, "KNN"))
    all_folds.append(cv["test_score"])

    # run logistic regression
    cv = cross_validate(LogisticRegression(), data, labels, cv=folds)
    output.append(create_output_row(cv, "LR"))
    all_folds.append(cv["test_score"])

    # run linear discriminant analysis
    cv = cross_validate(LinearDiscriminantAnalysis(), data, labels, cv=folds)
    output.append(create_output_row(cv, "LDA"))
    all_folds.append(cv["test_score"])

    # run multi layer perceptron
    cv = cross_validate(MLPClassifier(), data, labels, cv=folds)
    output.append(create_output_row(cv, "MLP"))
    all_folds.append(cv["test_score"])

    # run random forest
    cv = cross_validate(RandomForestClassifier(), data, labels, cv=folds)
    output.append(create_output_row(cv, "RF"))
    all_folds.append(cv["test_score"])

    sd = []
    avg = []
    for i in range(len(all_folds[0])):
        col = []
        col_avg = 0
        for j in range(len(all_folds)):
            col.append(all_folds[j][i])
            col_avg += all_folds[j][i]
        sd.append(statistics.stdev(col) * 100)
        avg.append(col_avg / len(all_folds) * 100)

    avg_sd = 0
    avg_avg = 0
    for i in range(len(sd)):
        avg_sd += sd[i]
        avg_avg += avg[i]
        sd[i] = ("{:.2f}%".format(sd[i]))
        avg[i] = ("{:.2f}%".format(avg[i]))
    sd.append("{:.2f}%".format(avg_sd / len(sd)))
    avg.append("{:.2f}%".format(avg_avg / len(avg)))
    sd.insert(0, "SD")
    avg.insert(0, "AVG")

    # add standard deviation and average
    output.append(sd)
    output.append(avg)

    # print results
    print(pd.DataFrame(output, columns=create_output_columns(folds)).to_string(index=False))
