package edu.cmu.ml.rtw.pra.graphs

import org.scalatest._

import edu.cmu.ml.rtw.pra.experiments.Dataset
import edu.cmu.ml.rtw.pra.experiments.Instance
import edu.cmu.ml.rtw.users.matt.util.FakeRandom

import org.json4s._
import org.json4s.JsonDSL._

class PprNegativeExampleSelectorSpec extends FlatSpecLike with Matchers {
  val params: JValue = ("ppr computer" -> ("type" -> "Fake"))
  val dataset = new Dataset(Seq(new Instance(1, 2, true, null)))
  val graph = new GraphOnDisk("src/test/resources/")

  "selectNegativeExamples" should "just add the sampled negatives to the given data" in {
    val selector = new PprNegativeExampleSelector(params, graph) {
      override def sampleByPrr(data: Dataset, pprValues: Map[Int, Map[Int, Int]]) = {
        Seq((1, 1), (2, 2), (3, 3))
      }
    }
    val withNegatives = selector.selectNegativeExamples(dataset, Set(), Set())
    withNegatives.getPositiveInstances should be(dataset.getPositiveInstances)
    val negativeInstances = withNegatives.getNegativeInstances
    negativeInstances.size should be(3)
    negativeInstances(0).source should be(1)
    negativeInstances(0).target should be(1)
    negativeInstances(0).isPositive should be(false)
    negativeInstances(1).source should be(2)
    negativeInstances(1).target should be(2)
    negativeInstances(1).isPositive should be(false)
    negativeInstances(2).source should be(3)
    negativeInstances(2).target should be(3)
    negativeInstances(2).isPositive should be(false)
  }

  "sampleByPrr" should "get enough negatives per positive" in {
    val selector = new PprNegativeExampleSelector(params, graph) {
      var index = -1
      var source = false
      override def weightedSample(list: Array[(Int, Int)], weight: Double, default: Int) = {
        if (source == false) index += 1
        source = !source
        list(index)._1
      }
    }
    val pprValues = Map(1 -> Map(3 -> 1, 4 -> 1, 5 -> 1), 2 -> Map(3 -> 1, 4 -> 1, 5 -> 1))
    val negatives = selector.sampleByPrr(dataset, pprValues)
    negatives.size should be(3)
    negatives.toSet should be(Set((3, 3), (4, 4), (5, 5)))
  }

  it should "give up when there aren't enough" in {
    val selector = new PprNegativeExampleSelector(params, graph) {
      override def weightedSample(list: Array[(Int, Int)], weight: Double, default: Int) = {
        1
      }
    }
    val pprValues = Map(1 -> Map(3 -> 1, 4 -> 1, 5 -> 1), 2 -> Map(3 -> 1, 4 -> 1, 5 -> 1))
    val negatives = selector.sampleByPrr(dataset, pprValues)
    negatives.size should be(1)
    negatives.toSet should be(Set((1, 1)))
  }

  it should "exclude training data" in {
    val selector = new PprNegativeExampleSelector(params, graph) {
      var index = -1
      var source = false
      override def weightedSample(list: Array[(Int, Int)], weight: Double, default: Int) = {
        if (source == false) index += 1
        source = !source
        if (index >= list.size) index = 0
        list(index)._1
      }
    }
    val pprValues = Map(1 -> Map(1 -> 1), 2 -> Map(2 -> 1))
    val negatives = selector.sampleByPrr(dataset, pprValues)
    negatives.size should be(0)
  }

  "weightedSample" should "give the right samples" in {
    val random = new FakeRandom
    val selector = new PprNegativeExampleSelector(params, graph, random)
    val list = Seq((1, 1), (2, 1), (3, 1)).toArray
    random.setNextDouble(.1)
    selector.weightedSample(list, 3.0, -1) should be(1)
    random.setNextDouble(.4)
    selector.weightedSample(list, 3.0, -1) should be(2)
    random.setNextDouble(.9)
    selector.weightedSample(list, 3.0, -1) should be(3)
    random.setNextDouble(.9)
    selector.weightedSample(list, 6.0, -1) should be(-1)
  }
}
