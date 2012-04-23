package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.dataparser.{GeoCoordinate, GeoCoordinateParser}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts geo-coodinates.
 */
class GeoExtractor( context : {
                        def ontology : Ontology
                        def redirects : Redirects  // redirects required by GeoCoordinateParser
                        def language : Language } ) extends Extractor
{
    private val geoCoordinateParser = new GeoCoordinateParser(context)

    private val typeOntProperty = context.ontology.getProperty("rdf:type").get
    private val latOntProperty = context.ontology.getProperty("geo:lat").get
    private val lonOntProperty = context.ontology.getProperty("geo:long").get
    private val pointOntProperty = context.ontology.getProperty("georss:point").get
    private val featureOntClass =  context.ontology.getClass("gml:_Feature").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Main) return new Graph()
        
        // Iterate through all root templates.
        // Not recursing into templates as these are presumed to be handled by template-based mechanisms (GeoCoordinatesMapping).
        for( templateNode @ TemplateNode(_, _, _) <- node.children;
             coordinate <- geoCoordinateParser.parse(templateNode) )
        {
            return writeGeoCoordinate(coordinate, subjectUri, node.sourceUri, pageContext)
        }

        new Graph()
    }

    private def writeGeoCoordinate(coord : GeoCoordinate, subjectUri : String, sourceUri : String, pageContext : PageContext) : Graph =
    {
        new Graph( new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, typeOntProperty, featureOntClass.uri, sourceUri) ::
                   new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, latOntProperty, coord.latitude.toString, sourceUri) ::
                   new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, lonOntProperty, coord.longitude.toString, sourceUri) ::
                   new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, pointOntProperty, coord.latitude + " " + coord.longitude, sourceUri) :: Nil )
    }
}
