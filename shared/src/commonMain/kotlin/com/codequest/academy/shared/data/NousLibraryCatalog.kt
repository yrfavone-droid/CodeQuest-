package com.codequest.academy.shared.data

/** Metadata for the verified offline PDF package. The PDFs themselves live in desktop resources. */
enum class LibraryKind { BOOK, INTENSIVE_FILE }

data class OfflineLibraryResource(
    val id: String,
    val kind: LibraryKind,
    val title: String,
    val subtitle: String,
    val pageCount: Int,
    val resourcePath: String,
    val sha256: String
)

object NousLibraryCatalog {
    const val packId = "nous-ai-academy-offline-v2"
    const val packageSha256 = "E32B1E9BB9409F9BA385CB30E85EABFCEC71EEE64BB9A75CDE3F9465335E2AB5"

    val resources = listOf(
        book("BOOK-01", "Python Foundations for Artificial Intelligence", "From first program to reliable AI-ready data tools", "book_01_python_foundations_for_artificial_intelligence.pdf", "1c7cabeaa50080060465864d471c9dbeb6eab9faf040884d84e3bc4fb16f4ced"),
        book("BOOK-02", "Mathematics for Machine Learning", "Visual intuition, derivation, and practical computation", "book_02_mathematics_for_machine_learning.pdf", "88b1ca9cf525c538b5e4831f56a315c4c3c1e286f740983c69a1a51f2a1e9352"),
        book("BOOK-03", "Algorithms, Data Structures, and AI Problem Solving", "Efficient reasoning from arrays to graph search", "book_03_algorithms_data_structures_and_ai_problem_solving.pdf", "1243a4dc6256b9e6d0a26794c22724f3bdc0d7cc1d02015ab312160a316ae905"),
        book("BOOK-04", "Machine Learning from First Principles", "Reliable classical models, evaluation, and interpretation", "book_04_machine_learning_from_first_principles.pdf", "f981a69cad53d83513aa2c231c45858455c9c823f9d9b683b2e2a93f208d4265"),
        book("BOOK-05", "Deep Learning, Generative AI, and Responsible Deployment", "Neural models from tensors to monitored products", "book_05_deep_learning_generative_ai_and_responsible_deployment.pdf", "a9ac3d7fa4f908ba1a73f036174e9a75d7d47b47260c75a4e34cc1e8336c03f2"),
        deep("DEEP-01", "Advanced Python Patterns", "Professional Python design and performance", "deep_01_advanced_python_patterns.pdf", "98cf700aa0db93a418c37929e9367ac18990b02d5a4e82ae8205be36a3577d8f"),
        deep("DEEP-02", "NumPy for Numerical Computing", "Efficient and stable array programming", "deep_02_numpy_for_numerical_computing.pdf", "d737461b1768ffb9c03b7a30dee05888461ba3792c8622e85067bc91c582900a"),
        deep("DEEP-03", "Pandas for Real Data", "Reliable table transformations", "deep_03_pandas_for_real_data.pdf", "094600c66abb7a3793c8424905dc0d98f61e3071edbc100195331aea1cb55be8"),
        deep("DEEP-04", "SQL for Data and AI", "Queries, feature tables, and data quality", "deep_04_sql_for_data_and_ai.pdf", "53f5035d1ba743b33e358e6b902e46557dbc4343e3e96352edfbb860d679e46c"),
        deep("DEEP-05", "Linear Algebra Deep Dive", "Geometry and matrix structure for AI", "deep_05_linear_algebra_deep_dive.pdf", "228126f196fc375fd0baa9a69ddb96fad2c28e17e3e9cdc295359424f38d5474"),
        deep("DEEP-06", "Probability and Statistics", "Uncertainty, estimation, and calibration", "deep_06_probability_and_statistics.pdf", "adf5bfb5f12206cd39cd4b33ea741a9cf1e696136707a7e649911f6695bd2a76"),
        deep("DEEP-07", "Calculus and Optimization", "Derivatives and stable learning", "deep_07_calculus_and_optimization.pdf", "bb2a2b0447ff39cfef63f5386bede18df375e52b33d82c7af10ffa71ddcdb35c"),
        deep("DEEP-08", "Data Structures in Production", "Choosing structures under real constraints", "deep_08_data_structures_in_production.pdf", "6f3a44cb6a9a81c3ff5e83011bad4724e80b12c3a550643cf3561ac6543a9604"),
        deep("DEEP-09", "Algorithm Design Patterns", "Reusable strategies for hard problems", "deep_09_algorithm_design_patterns.pdf", "a34b614aedb1732e22da6639a242c59f0e65fd1e15452ecae911a808b0c2aa7f"),
        deep("DEEP-10", "Exploratory Analysis and Visualization", "Evidence-first data communication", "deep_10_exploratory_analysis_and_visualization.pdf", "f29311ac12e68ffac4a773ce707241f734ebb364648b233f21138d85c824e83e"),
        deep("DEEP-11", "Preprocessing and Feature Engineering", "Leakage-safe transformations", "deep_11_preprocessing_and_feature_engineering.pdf", "fdcc143df822302fc6bc1c4f2f3276f2947d8f4cf2e0eb428132b09ad266fc9c"),
        deep("DEEP-12", "Supervised Learning", "Losses, models, and diagnostics", "deep_12_supervised_learning.pdf", "708cb5eaff0f09ae86d4f10aba9c25ac4b7f8358903252b6ccbe8d9b5511d547"),
        deep("DEEP-13", "Unsupervised Learning", "Discovering and testing structure", "deep_13_unsupervised_learning.pdf", "670b62191c33c0617232a2509eefc2b4fa9fe09ca1935fa8f84d0e1d8f509cba"),
        deep("DEEP-14", "Time Series Forecasting", "Respecting time in prediction", "deep_14_time_series_forecasting.pdf", "fb2e58ac7fb1f56646f972ba1620a5be7ccbff4602b93491130a9ed64ba603fa"),
        deep("DEEP-15", "Deep Learning with PyTorch", "Transparent neural training loops", "deep_15_deep_learning_with_pytorch.pdf", "b75f66e46182520eed829493bdde19130ca3644555a15ac52f7f25c0c1dde7ad"),
        deep("DEEP-16", "Computer Vision Systems", "Images from representation to deployment", "deep_16_computer_vision_systems.pdf", "d13d2d7b6b7ba49583a45f1d0b119febada2a0a3ae91b2c4971cb7b196a2f0c0"),
        deep("DEEP-17", "Natural Language Processing", "Text representations and evaluation", "deep_17_natural_language_processing.pdf", "884039d8e9faa9cc8f85c94b5201462375f9770c28fa0ff077d95c5f096e3a20"),
        deep("DEEP-18", "Transformers, LLMs, and Retrieval", "Grounded language systems", "deep_18_transformers_llms_and_retrieval.pdf", "e46d06f1a9b417a856c4edc503212684e372d6349cf09e050deb8cde0e6f34d7"),
        deep("DEEP-19", "Reinforcement Learning", "Sequential decisions and safe evaluation", "deep_19_reinforcement_learning.pdf", "f58d5e6e5a4265ac061527e8e26d35149aa43a1b9ed784abd9d405f25d69b57f"),
        deep("DEEP-20", "MLOps and Responsible AI", "Reproducible, monitored, and accountable systems", "deep_20_mlops_and_responsible_ai.pdf", "a7718164fa9adb630daa8f687d7a774f55bef0b48216a191f85bbe06aabb85ec")
    )

    private fun book(id: String, title: String, subtitle: String, file: String, sha256: String) = OfflineLibraryResource(id, LibraryKind.BOOK, title, subtitle, 150, "content/books/$file", sha256)
    private fun deep(id: String, title: String, subtitle: String, file: String, sha256: String) = OfflineLibraryResource(id, LibraryKind.INTENSIVE_FILE, title, subtitle, 50, "content/deep_dives/$file", sha256)
}
